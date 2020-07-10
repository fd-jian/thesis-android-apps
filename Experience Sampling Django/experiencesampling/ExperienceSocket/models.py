import json
from django.db import models
from django import forms
from django.contrib.auth.models import AbstractUser


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


    QUESTION_TYPE = [
        ("NumAns", "Numerical Answer"),
        ("TxtAns", "Text Answer"),
    ]

    question_type = models.CharField(
        max_length=6,
        choices=QUESTION_TYPE,
        default="TxtAns",
    )

    numerical_answer_lower = models.IntegerField(help_text="Lower Bound", blank=True, null=True)
    numerical_answer_upper = models.IntegerField(help_text="Upper Bound", blank=True, null=True)

    text_answer = models.CharField(max_length=256, help_text="zB: 'Ja, Vielleicht, Nein' oder leer lassen falls per Voice geantwortet werden soll", blank=True, null=True)

    NUMBERS = list(range(1,351))
    TIMING_ANSWERS = [
        ("Begin", "Start"),
        ("End", "End"),
    ]
    for i in NUMBERS:
        TIMING_ANSWERS.append((str(i),str(i)))

    schedule = models.CharField(
        max_length=15,
        choices=TIMING_ANSWERS,
        default="Begin",
        help_text="Zeit in Minuten nach dem Start"
    )

    def __str__(self):
        return self.question














class User(AbstractUser):
    pass
