"""experiencesampling URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/3.0/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
    python manage.py runserver 0.0.0.0:8000
"""
from django.contrib import admin
from django.urls import path
from django.conf.urls import include

from ExperienceSocket.views import SignUpView
from ExperienceSocket.views import LogInView
from ExperienceSocket.views import LogOutView

urlpatterns = [
    path('admin/', admin.site.urls),
    path('ExperienceSocket/', include('ExperienceSocket.urls')),

    #path('signup',SignUpView.as_view(),name='sign_up'),
    #path('login',LogInView.as_view(), name='log_in'),
    #path('logout', LogOutView.as_view(), name='log_out'),
]
