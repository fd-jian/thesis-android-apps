import json
from django.db import models
from django import forms
from django.contrib.auth.models import AbstractUser

# Constant Values
## Scheduling
TIMING_ANSWERS = [
    ("0", "Start"),
    ("9999", "End"),
]
NUMBERS = list(range(1, 351))
for i in NUMBERS:
    TIMING_ANSWERS.append((str(i), str(i)))

## Question Types
QUESTION_TYPE = [
    ("NumAns", "Numerical Answer"),
    ("TxtAns", "Text Answer"),
]

SURVEY_TYPE = [
    ("single", "Single Survey"),
    ("interval", "Interval Survey"),
]



class Message(models.Model):
    content = models.TextField()
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return (str(self.timestamp) + " " + self.content)

    def pretty(self):
        return json.dumps({
            'Timestamp': str(self.timestamp),
            'Message': self.content
        }, indent=4)

    def last_10_messages(self):
        return Message.objects.order_by('-timestamp').all()[:10]

class Question(models.Model):
    question = models.CharField(max_length=256)
    answers = models.TextField()
    origin = models.TextField(null=True, blank=True, )
    num_answers = models.IntegerField()

    def __str__(self):
        return json.dumps({
            'Question ': self.question,
            'Answers ': self.answers,
            'Origin ': self.origin,
            'num_answers': self.num_answers
        })

class M1_Question(models.Model):
    question = models.CharField(max_length=256, help_text="zB: Wie geht es dir?")

    question_type = models.CharField(
        max_length=6,
        choices=QUESTION_TYPE,
        default="TxtAns",
    )

    numerical_answer_lower = models.IntegerField(help_text="Lower Bound", blank=True, null=True)
    numerical_answer_upper = models.IntegerField(help_text="Upper Bound", blank=True, null=True)

    text_answer = models.CharField(max_length=256, help_text="zB: 'Ja, Vielleicht, Nein' oder leer lassen falls per Voice geantwortet werden soll", blank=True, null=True)


    schedule = models.CharField(
        max_length=15,
        choices=TIMING_ANSWERS,
        default="Begin",
        help_text="Zeit in Minuten nach dem Start"
    )

    def __str__(self):
        return "Question: {2: <15} ({0: <6}, schedule= {1: <5})".format(self.question_type,self.schedule,self.question)


class Survey(models.Model):
    name = models.CharField(max_length=256)
    survey_type = models.CharField(
        max_length=15,
        choices=SURVEY_TYPE,
        default="single",
        help_text="Je nach Typ ist das Scheduling in Intervallen (exklusiv Start & End!) oder einmalig."
    )
    schedule = models.CharField(
        max_length=15,
        choices=TIMING_ANSWERS,
        default="Begin",
        help_text="Zeit in Minuten nach dem Start"
    )

    def __str__(self):
        return "Survey: {0: <15} ({2: <6}, schedule= {1: <5})".format(self.name, self.schedule, self.survey_type)

class SurveyQuestion(models.Model):
    survey = models.ForeignKey(Survey, on_delete=models.CASCADE)
    question = models.CharField(max_length=256, help_text="zB: Wie geht es dir?")

    question_type = models.CharField(
        max_length=6,
        choices=QUESTION_TYPE,
        default="TxtAns",
    )

    numerical_answer_lower = models.IntegerField(help_text="Lower Bound", blank=True, null=True)
    numerical_answer_upper = models.IntegerField(help_text="Upper Bound", blank=True, null=True)

    text_answer = models.CharField(max_length=256,
                                   help_text="zB: 'Ja, Vielleicht, Nein' oder leer lassen falls per Voice geantwortet werden soll",
                                   blank=True, null=True)


class User(AbstractUser):
    pass
