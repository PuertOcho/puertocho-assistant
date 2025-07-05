# Guía de respaldo y restauración para Raspberry Pi 3

> Última actualización: 2025-07-03  
> Autor: Puertocho

Esta guía describe varias estrategias para clonar, respaldar y restaurar una Raspberry Pi 3 que ya tiene **Docker** y **Git** configurados. El objetivo es ahorrar tiempo cuando el sistema falle o quieras desplegar rápidamente nuevas tarjetas SD con el entorno listo para usar.

---

## 1 · Crear una *Golden Image* (clonado completo de la SD)

1. **Preparar el sistema**  
   – Instala Raspberry Pi OS (64-bit recomendado).  
   – Configura usuario, SSH, Docker y Git.  
   – Apaga la Raspberry (`sudo poweroff`).
2. **Clonar la tarjeta desde otro equipo Linux/macOS**  
   ```bash
   lsblk                                     # identifica tu SD, p. ej. /dev/sdb
   sudo dd if=/dev/sdX of=~/raspi_docker_git_2025-07-03.img \
            bs=4M status=progress
   sudo sync                                 # asegura que se escriban los buffers
   ```
3. **Almacenar la imagen**  
   Copia `raspi_docker_git_2025-07-03.img` en un disco externo, NAS o nube.
4. **Restaurar cuando sea necesario**  
   Graba la imagen en una SD nueva con Raspberry Pi Imager, Balena Etcher o `dd`.

**Ventajas**  
• Restauración rapidísima.  
• Incluye todo: paquetes, claves, configuraciones.  
**Desventajas**  
• Tamaño grande (ocupa toda la SD).  
• Debes generar una nueva imagen cada vez que modifiques el sistema.

---

## 2 · rpi-clone (clonación incremental "en caliente")

`rpi-clone` permite duplicar la SD en otro dispositivo (USB/SD) sin apagar la Pi.

```bash
curl -L https://raw.githubusercontent.com/billw2/rpi-clone/master/rpi-clone \
     -o ~/rpi-clone && chmod +x ~/rpi-clone
sudo ./rpi-clone sda  # sustituye "sda" por tu pendrive/disco
```

Puedes programar una tarea `cron` semanal para mantener un clon actualizado. Si la SD falla, basta con arrancar desde el clon.

---

## 3 · Imágenes reproducibles con **pi-gen** o **Packer**

Para entornos DevOps o equipos múltiples:

1. Clona el repositorio oficial:
   ```bash
   git clone https://github.com/RPi-Distro/pi-gen.git
   ```
2. Crea un *stage* personalizado donde instales Docker y Git mediante scripts.  
3. Ejecuta `pi-gen` desde tu PC para generar un `.img` listo.

Ventajas: receta versionada en Git; ideal para CI/CD.  
Desventajas: curva de aprendizaje mayor.

---

## 4 · Automatización post-instalación (script Bash o Ansible)

Mantén un script, por ejemplo `setup_pi.sh`:

```bash
#!/usr/bin/env bash
set -e
sudo apt update && sudo apt -y upgrade

# Instalar Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"

# Instalar Git
sudo apt -y install git

# Otras herramientas
# sudo apt -y install htop vim

# Copiar dotfiles o claves si las tienes en un repo
# git clone https://github.com/tuusuario/dotfiles.git ~/dotfiles && ~/dotfiles/install.sh
```

Ejecútalo tras un flashing limpio; en ~10 min tendrás la Pi lista.

---

## 5 · Respaldar **datos** vs. **sistema**

Aunque tengas una imagen, conviene respaldar datos que cambian a diario:

- Directorios de usuario (`/home`), bases de datos, volúmenes Docker.
- Listado de paquetes:
  ```bash
  dpkg --get-selections > /home/$USER/pkg_list.txt
  ```
- Para restaurar: `dpkg --set-selections < pkg_list.txt && sudo apt dselect-upgrade`.

Herramientas útiles: `rsync`, `duplicity`, `restic`, `borgbackup`.

---

## 6 · Estrategia recomendada

1. **Golden Image** una vez que tengas Docker y Git funcionando.  
2. **Script post-instalación** para reproducibilidad y cambios menores.  
3. **Backups regulares** de datos/vólumenes.  
4. (Opcional) **rpi-clone** si dispones de almacenamiento USB permanente.

Así te aseguras una restauración rápida y sin sorpresas, mientras mantienes la flexibilidad para actualizar tu entorno cuando lo necesites.

---

### Referencias

- pi-gen: <https://github.com/RPi-Distro/pi-gen>
- rpi-clone: <https://github.com/billw2/rpi-clone>
- Docker install script: <https://get.docker.com>

---

**¡Listo!** Con estas prácticas tendrás tu Raspberry Pi siempre preparada y ahorrarás tiempo en cada reinstalación. 