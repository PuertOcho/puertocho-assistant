import os
import requests
from flask import Flask, request, jsonify, send_file
import whisper
import torch
import tempfile
import shutil
from datetime import datetime

# Se asegura de usar CUDA
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
MODEL_NAME = os.getenv("WHISPER_MODEL", "base")
HOST = os.getenv("FLASK_HOST", "0.0.0.0")
PORT = int(os.getenv("FLASK_PORT", 5000))
DEFAULT_LANGUAGE = os.getenv("DEFAULT_LANGUAGE", "es")  # Español por defecto
DEBUG_AUDIO = os.getenv("DEBUG_AUDIO", "true").lower() == "true"  # Guardar audio para debug

# Configuración de API externa
ENABLE_EXTERNAL_API = os.getenv("ENABLE_EXTERNAL_API", "false").lower() == "true"
EXTERNAL_API_URL = os.getenv("EXTERNAL_API_URL", "https://api.openai.com/v1/audio/transcriptions")
EXTERNAL_API_KEY = os.getenv("EXTERNAL_API_KEY", "")
EXTERNAL_API_MODEL = os.getenv("EXTERNAL_API_MODEL", "whisper-1")

# Configuración de método por defecto y fallback
DEFAULT_TRANSCRIPTION_METHOD = os.getenv("DEFAULT_TRANSCRIPTION_METHOD", "local").lower()
FALLBACK_METHOD = os.getenv("FALLBACK_METHOD", "local").lower()
FALLBACK_ENABLED = os.getenv("FALLBACK_ENABLED", "true").lower() == "true"
EXTERNAL_API_TIMEOUT = int(os.getenv("EXTERNAL_API_TIMEOUT", "30"))

# Validar configuración
VALID_METHODS = ["local", "external"]
if DEFAULT_TRANSCRIPTION_METHOD not in VALID_METHODS:
    print(f"[WARNING] Método por defecto inválido: {DEFAULT_TRANSCRIPTION_METHOD}, usando 'local'")
    DEFAULT_TRANSCRIPTION_METHOD = "local"

if FALLBACK_METHOD not in VALID_METHODS:
    print(f"[WARNING] Método de fallback inválido: {FALLBACK_METHOD}, usando 'local'")
    FALLBACK_METHOD = "local"

# Crear directorio para audio de debug
DEBUG_DIR = "/app/debug_audio"
if DEBUG_AUDIO and not os.path.exists(DEBUG_DIR):
    os.makedirs(DEBUG_DIR, exist_ok=True)

print(f"[*] Usando dispositivo: {DEVICE}, cargando modelo: {MODEL_NAME}")
print(f"[*] Idioma por defecto: {DEFAULT_LANGUAGE}")
print(f"[*] Debug de audio: {'ACTIVADO' if DEBUG_AUDIO else 'DESACTIVADO'}")
print(f"[*] API externa: {'ACTIVADA' if ENABLE_EXTERNAL_API else 'DESACTIVADA'}")
print(f"[*] Método por defecto: {DEFAULT_TRANSCRIPTION_METHOD}")
print(f"[*] Método de fallback: {FALLBACK_METHOD}")
print(f"[*] Fallback habilitado: {'ACTIVADO' if FALLBACK_ENABLED else 'DESACTIVADO'}")
if ENABLE_EXTERNAL_API:
    print(f"[*] URL API externa: {EXTERNAL_API_URL}")
    print(f"[*] Modelo API externa: {EXTERNAL_API_MODEL}")
if DEBUG_AUDIO:
    print(f"[*] Directorio de debug: {DEBUG_DIR}")

# Cargar modelo local solo si es necesario
model = None
if DEFAULT_TRANSCRIPTION_METHOD == "local" or FALLBACK_METHOD == "local":
    model = whisper.load_model(MODEL_NAME, device=DEVICE)

app = Flask(__name__)

def transcribe_local(audio_file, language):
    """Transcripción usando modelo local"""
    if model is None:
        raise Exception("Modelo local no disponible")
    
    print(f"[LOCAL] Transcribiendo con idioma: {language}")
    res = model.transcribe(audio_file, language=language)
    transcription = res["text"].strip()
    print(f"[LOCAL] Transcripción obtenida: '{transcription}'")
    
    return {
        "transcription": transcription,
        "language": language,
        "detected_language": res.get("language", "unknown"),
        "method": "local"
    }

def transcribe_external(audio_file, language):
    """Transcripción usando API externa"""
    if not EXTERNAL_API_KEY:
        raise Exception("API key no configurada")
    
    print(f"[EXTERNAL] Transcribiendo con API externa, idioma: {language}")
    
    headers = {
        "Authorization": f"Bearer {EXTERNAL_API_KEY}"
    }
    
    files = {
        "file": ("audio.wav", open(audio_file, "rb"), "audio/wav"),
        "model": (None, EXTERNAL_API_MODEL),
        "language": (None, language),
        "response_format": (None, "json")
    }
    
    try:
        response = requests.post(
            EXTERNAL_API_URL,
            headers=headers,
            files=files,
            timeout=EXTERNAL_API_TIMEOUT
        )
        
        if response.status_code == 200:
            result = response.json()
            transcription = result.get("text", "").strip()
            print(f"[EXTERNAL] Transcripción obtenida: '{transcription}'")
            
            return {
                "transcription": transcription,
                "language": language,
                "detected_language": language,  # La API externa usa el idioma especificado
                "method": "external"
            }
        else:
            error_msg = f"API externa falló: {response.status_code} - {response.text}"
            print(f"[EXTERNAL] {error_msg}")
            raise Exception(error_msg)
            
    except requests.exceptions.Timeout:
        error_msg = "Timeout en API externa"
        print(f"[EXTERNAL] {error_msg}")
        raise Exception(error_msg)
    except Exception as e:
        error_msg = f"Error en API externa: {str(e)}"
        print(f"[EXTERNAL] {error_msg}")
        raise Exception(error_msg)

def get_transcription_method(use_external_param=None):
    """Determina el método de transcripción basado en configuración y parámetros"""
    # Si se especifica explícitamente en la petición, usar ese
    if use_external_param is not None:
        return "external" if use_external_param else "local"
    
    # Si no se especifica, usar el método por defecto configurado
    return DEFAULT_TRANSCRIPTION_METHOD

def can_use_method(method):
    """Verifica si se puede usar un método específico"""
    if method == "external":
        return ENABLE_EXTERNAL_API and EXTERNAL_API_KEY
    elif method == "local":
        return model is not None
    return False

@app.route("/transcribe", methods=["POST"])
def transcribe():
    if "audio" not in request.files:
        return jsonify({"error": "No audio file"}), 400

    # Obtener parámetros
    language = request.form.get("language", DEFAULT_LANGUAGE)
    use_external = request.form.get("use_external")
    
    # Convertir a boolean si se proporciona
    if use_external is not None:
        use_external = use_external.lower() == "true"
    
    # Determinar método de transcripción
    transcription_method = get_transcription_method(use_external)
    
    f = request.files["audio"]
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    f.save(tmp.name)

    # Guardar audio para debug si está activado
    debug_filename = None
    if DEBUG_AUDIO:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]  # milisegundos
        debug_filename = f"audio_{timestamp}.wav"
        debug_path = os.path.join(DEBUG_DIR, debug_filename)
        shutil.copy2(tmp.name, debug_path)
        print(f"[DEBUG] Audio guardado: {debug_filename}")

    try:
        # Intentar transcripción con el método seleccionado
        if can_use_method(transcription_method):
            try:
                if transcription_method == "external":
                    result = transcribe_external(tmp.name, language)
                else:
                    result = transcribe_local(tmp.name, language)
            except Exception as e:
                # Si falla el método principal y hay fallback habilitado
                if FALLBACK_ENABLED and can_use_method(FALLBACK_METHOD) and FALLBACK_METHOD != transcription_method:
                    print(f"[FALLBACK] Cambiando de {transcription_method} a {FALLBACK_METHOD}: {e}")
                    if FALLBACK_METHOD == "external":
                        result = transcribe_external(tmp.name, language)
                    else:
                        result = transcribe_local(tmp.name, language)
                    result["fallback"] = True
                    result["fallback_reason"] = str(e)
                    result["original_method"] = transcription_method
                else:
                    raise e
        else:
            # Si el método principal no está disponible, intentar con el fallback
            if FALLBACK_ENABLED and can_use_method(FALLBACK_METHOD):
                print(f"[FALLBACK] Método {transcription_method} no disponible, usando {FALLBACK_METHOD}")
                if FALLBACK_METHOD == "external":
                    result = transcribe_external(tmp.name, language)
                else:
                    result = transcribe_local(tmp.name, language)
                result["fallback"] = True
                result["fallback_reason"] = f"Método {transcription_method} no disponible"
                result["original_method"] = transcription_method
            else:
                raise Exception(f"Método {transcription_method} no disponible y fallback deshabilitado")
        
        response_data = {
            "transcription": result["transcription"],
            "language": result["language"],
            "detected_language": result["detected_language"],
            "method": result["method"]
        }
        
        # Incluir información de fallback si aplica
        if result.get("fallback", False):
            response_data["fallback"] = True
            response_data["fallback_reason"] = result["fallback_reason"]
            response_data["original_method"] = result["original_method"]
        
        # Incluir información del archivo de debug si está activado
        if DEBUG_AUDIO and debug_filename:
            response_data["debug_audio_file"] = debug_filename
            response_data["debug_audio_url"] = f"/debug/audio/{debug_filename}"
        
        return jsonify(response_data)
    except Exception as e:
        print(f"[!] Error en transcripción: {e}")
        return jsonify({"error": str(e)}), 500
    finally:
        os.unlink(tmp.name)

@app.route("/transcribe/local", methods=["POST"])
def transcribe_local_endpoint():
    """Endpoint específico para transcripción local"""
    if "audio" not in request.files:
        return jsonify({"error": "No audio file"}), 400

    language = request.form.get("language", DEFAULT_LANGUAGE)
    
    f = request.files["audio"]
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    f.save(tmp.name)

    # Guardar audio para debug si está activado
    debug_filename = None
    if DEBUG_AUDIO:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]
        debug_filename = f"audio_{timestamp}.wav"
        debug_path = os.path.join(DEBUG_DIR, debug_filename)
        shutil.copy2(tmp.name, debug_path)
        print(f"[DEBUG] Audio guardado: {debug_filename}")

    try:
        result = transcribe_local(tmp.name, language)
        
        response_data = {
            "transcription": result["transcription"],
            "language": result["language"],
            "detected_language": result["detected_language"],
            "method": "local"
        }
        
        if DEBUG_AUDIO and debug_filename:
            response_data["debug_audio_file"] = debug_filename
            response_data["debug_audio_url"] = f"/debug/audio/{debug_filename}"
        
        return jsonify(response_data)
    except Exception as e:
        print(f"[!] Error en transcripción local: {e}")
        return jsonify({"error": str(e)}), 500
    finally:
        os.unlink(tmp.name)

@app.route("/transcribe/external", methods=["POST"])
def transcribe_external_endpoint():
    """Endpoint específico para transcripción externa"""
    if not ENABLE_EXTERNAL_API:
        return jsonify({"error": "API externa no habilitada"}), 400
    
    if "audio" not in request.files:
        return jsonify({"error": "No audio file"}), 400

    language = request.form.get("language", DEFAULT_LANGUAGE)
    
    f = request.files["audio"]
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    f.save(tmp.name)

    # Guardar audio para debug si está activado
    debug_filename = None
    if DEBUG_AUDIO:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]
        debug_filename = f"audio_{timestamp}.wav"
        debug_path = os.path.join(DEBUG_DIR, debug_filename)
        shutil.copy2(tmp.name, debug_path)
        print(f"[DEBUG] Audio guardado: {debug_filename}")

    try:
        result = transcribe_external(tmp.name, language)
        
        response_data = {
            "transcription": result["transcription"],
            "language": result["language"],
            "detected_language": result["detected_language"],
            "method": "external"
        }
        
        if DEBUG_AUDIO and debug_filename:
            response_data["debug_audio_file"] = debug_filename
            response_data["debug_audio_url"] = f"/debug/audio/{debug_filename}"
        
        return jsonify(response_data)
    except Exception as e:
        print(f"[!] Error en transcripción externa: {e}")
        if FALLBACK_ENABLED and can_use_method(FALLBACK_METHOD):
            print(f"[FALLBACK] Intentando transcripción con {FALLBACK_METHOD}...")
            try:
                if FALLBACK_METHOD == "external":
                    result = transcribe_external(tmp.name, language)
                else:
                    result = transcribe_local(tmp.name, language)
                response_data = {
                    "transcription": result["transcription"],
                    "language": result["language"],
                    "detected_language": result["detected_language"],
                    "method": FALLBACK_METHOD,
                    "fallback": True,
                    "fallback_reason": str(e)
                }
                
                if DEBUG_AUDIO and debug_filename:
                    response_data["debug_audio_file"] = debug_filename
                    response_data["debug_audio_url"] = f"/debug/audio/{debug_filename}"
                
                return jsonify(response_data)
            except Exception as local_error:
                return jsonify({"error": f"Error en API externa y fallback {FALLBACK_METHOD}: {str(e)} -> {str(local_error)}"}), 500
        else:
            return jsonify({"error": str(e)}), 500
    finally:
        os.unlink(tmp.name)

@app.route("/status", methods=["GET"])
def status():
    """Endpoint para verificar el estado del servicio"""
    status_info = {
        "status": "ok",
        "device": DEVICE,
        "local_model": MODEL_NAME,
        "default_language": DEFAULT_LANGUAGE,
        "debug_audio": DEBUG_AUDIO,
        "external_api_enabled": ENABLE_EXTERNAL_API,
        "default_transcription_method": DEFAULT_TRANSCRIPTION_METHOD,
        "fallback_method": FALLBACK_METHOD,
        "fallback_enabled": FALLBACK_ENABLED
    }
    
    if ENABLE_EXTERNAL_API:
        status_info["external_api_url"] = EXTERNAL_API_URL
        status_info["external_api_model"] = EXTERNAL_API_MODEL
        status_info["external_api_configured"] = bool(EXTERNAL_API_KEY)
    
    return jsonify(status_info)

@app.route("/debug/audio/<filename>", methods=["GET"])
def get_debug_audio(filename):
    """Endpoint para descargar/reproducir archivos de audio de debug"""
    if not DEBUG_AUDIO:
        return jsonify({"error": "Debug audio is disabled"}), 404
    
    file_path = os.path.join(DEBUG_DIR, filename)
    if not os.path.exists(file_path):
        return jsonify({"error": "Audio file not found"}), 404
    
    return send_file(file_path, mimetype="audio/wav", as_attachment=False)

@app.route("/debug/audio", methods=["GET"])
def list_debug_audio():
    """Endpoint para listar todos los archivos de audio de debug"""
    if not DEBUG_AUDIO:
        return jsonify({"error": "Debug audio is disabled"}), 404
    
    try:
        files = []
        if os.path.exists(DEBUG_DIR):
            for filename in sorted(os.listdir(DEBUG_DIR), reverse=True):
                if filename.endswith('.wav'):
                    file_path = os.path.join(DEBUG_DIR, filename)
                    file_stats = os.stat(file_path)
                    files.append({
                        "filename": filename,
                        "size": file_stats.st_size,
                        "created": datetime.fromtimestamp(file_stats.st_mtime).isoformat(),
                        "url": f"/debug/audio/{filename}"
                    })
        
        return jsonify({
            "files": files,
            "total": len(files),
            "debug_dir": DEBUG_DIR
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/health", methods=["GET"])
def health():
    """Endpoint de verificación de estado (health check)"""
    return jsonify({"status": "ok"}), 200

if __name__ == "__main__":
    app.run(host=HOST, port=PORT)
