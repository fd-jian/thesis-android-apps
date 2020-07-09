from django.shortcuts import render

def index(request):
    return render(request,'ExperienceSocket/index.html', {})


def room(request, room_name):
    return render(request, 'ExperienceSocket/room.html', {
        'room_name': room_name
    })