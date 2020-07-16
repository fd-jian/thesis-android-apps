# Experience 
## Django Backend
### Admin Account für ../admin
Zum erstellen eines Admin Accounts muss der Server nicht laufen. Auf dem
localhost reicht es, wenn man in den experiencesampling Ordner navigiert in dem
sich auch die 'manage.py' befindet. Hierfür in die cmd wechseln und den Befehl
"python manage.py createsuperuser" ausführen. Für den Docker Container muss 
man zunächst in diesen wechseln mit dem Befehl "docker-compose exec web /bin/bash".
Anschließend ersten Befehl ausführen.
### Neue Models erstellen und auf der Admin Page Registrieren.
Um neue Models zu erstellen müssen diese in der ExperienceSocket/models.py 
erstellt werden. Anschließend müssen diese in der ExperienceSocket/admin.py
registriert werden. Beispiele sind dort zu finden. Bevor man diese sehen kann
sollten die Befehle "python manage.py makemigration" und "python manage.py migrate"
ausgeführt werden, damit die Änderungen übernommen werden. Bei einzelnen Änderungen
innerhalb einer Klasse muss man dies nicht machen. 
## Android Application

## Old
### Wiki Google Doc
[Doc Link](https://docs.google.com/spreadsheets/d/1un-965-LAptT_QFSWqjXU4S76e3s4ppeOEl8tEc_oYo/edit?usp=sharing)








