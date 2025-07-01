import os
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

# Crear directorio para audio de debug
DEBUG_DIR = "/app/debug_audio"
if DEBUG_AUDIO and not os.path.exists(DEBUG_DIR):
    os.makedirs(DEBUG_DIR, exist_ok=True)

print(f"[*] Usando dispositivo: {DEVICE}, cargando modelo: {MODEL_NAME}")
print(f"[*] Idioma por defecto: {DEFAULT_LANGUAGE}")
print(f"[*] Debug de audio: {'ACTIVADO' if DEBUG_AUDIO else 'DESACTIVADO'}")
if DEBUG_AUDIO:
    print(f"[*] Directorio de debug: {DEBUG_DIR}")

model = whisper.load_model(MODEL_NAME, device=DEVICE)

app = Flask(__name__)

@app.route("/transcribe", methods=["POST"])
def transcribe():
    if "audio" not in request.files:
        return jsonify({"error": "No audio file"}), 400

    # Obtener el idioma desde los parámetros de la petición o usar el predeterminado
    language = request.form.get("language", DEFAULT_LANGUAGE)
    
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
        # Transcribir con el idioma especificado
        print(f"[*] Transcribiendo con idioma: {language}")
        res = model.transcribe(tmp.name, language=language)
        transcription = res["text"].strip()
        print(f"[*] Transcripción obtenida: '{transcription}'")
        
        response_data = {
            "transcription": transcription,
            "language": language,
            "detected_language": res.get("language", "unknown")
        }
        
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
