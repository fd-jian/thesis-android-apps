from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as DefaultUserAdmin
from .models import *


admin.site.register(Question)


# Register Milestone 1 Question


class SurveyQuestionsInline(admin.StackedInline):
    model = SurveyQuestion
    extra = 0

class SurveyAdmin(admin.ModelAdmin):
    fieldsets = [

        ("General",             {'fields': ['name', 'survey_type', 'schedule']}),

    ]

    inlines = [SurveyQuestionsInline,]

admin.site.register(Survey, SurveyAdmin)




class QuestionAdmin(admin.ModelAdmin):
    fieldsets = [

        ("General",             {'fields': ['question','question_type']}),
        ("Text Question",       {'fields': ['text_answer']}),
        ("Numerical Question",  {'fields': ['numerical_answer_lower','numerical_answer_upper']}),
        ("Scheduling",          {'fields': ['schedule']})

    ]
admin.site.register(M1_Question,QuestionAdmin)











# received Messages from handheld Model
admin.site.register(Message)

# LogIn & Co
#@admin.register(User)
#class UserAdmin(DefaultUserAdmin):
#    pass