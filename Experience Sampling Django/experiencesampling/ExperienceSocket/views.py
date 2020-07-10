# WebSocket
from django.shortcuts import render
from rest_framework import generics
from django.contrib.auth import get_user_model

# User Authentification
from rest_framework import generics
from rest_framework.response import Response
from .serializer import UserSerializer
from django.contrib.auth import get_user_model, login, logout
from django.contrib.auth.forms import AuthenticationForm
from rest_framework import generics, permissions, status, views

def index(request):
    return render(request,'ExperienceSocket/index.html', {})


def room(request, room_name):
    return render(request, 'ExperienceSocket/room.html', {
        'room_name': room_name
    })

class SignUpView(generics.CreateAPIView):
    queryset = get_user_model().objects.all()
    serializer_class = UserSerializer

class LogInView(views.APIView):
    def post(self, request):
        form = AuthenticationForm(data=request.data)
        if form.is_valid():
            user = form.get_user()
            login(request, user=form.get_user())
            return Response(UserSerializer(user).data)
        else:
            return Response(form.errors, status=status.HTTP_400_BAD_REQUEST)


# new
class LogOutView(views.APIView):
    permission_classes = (permissions.IsAuthenticated,)

    def post(self, *args, **kwargs):
        logout(self.request)
        return Response(status=status.HTTP_204_NO_CONTENT)