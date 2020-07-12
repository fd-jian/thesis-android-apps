from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as DefaultUserAdmin
from .models import *
# Register your models here.

admin.site.register(Question)
admin.site.register(Message)
admin.site.register(M1_Question)
admin.site.register(ScheduledTasks)



@admin.register(User)
class UserAdmin(DefaultUserAdmin):
    pass