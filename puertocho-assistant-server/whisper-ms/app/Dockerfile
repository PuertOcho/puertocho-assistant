# 1. Imagen oficial de PyTorch con CUDA y cuDNN
FROM pytorch/pytorch:2.1.0-cuda11.8-cudnn8-runtime

# 2. Evitar prompts interactivos de apt (p. ej. tzdata)
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/Madrid

WORKDIR /app

# 3. Instala dependencias del sistema (incluyendo git, ffmpeg y tzdata)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      git \
      ffmpeg \
      tzdata && \
    rm -rf /var/lib/apt/lists/*

# 4. Copia e instala dependencias Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. Copia el código de la aplicación
COPY . .

# 6. Expone el puerto de Flask
EXPOSE ${FLASK_PORT}

# 7. Ejecuta la aplicación
CMD ["python", "app.py"]

