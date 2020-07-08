from django.db import models
import json

class Message(models.Model):
    content = models.TextField()
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return (str(self.timestamp) +" "+ self.content)

    def pretty(self):
        return json.dumps({
            'Timestamp': str(self.timestamp),
            'Message': self.content
        }, indent=4)

    def last_10_messages(self):
        return Message.objects.order_by('-timestamp').all()[:10]


class Questions(models.Model):
    question = models.TextField()
    answers = models.TextField()
    origin = models.TextField()
    num_answers = models.IntegerField()

    def __str__(self):
        return json.dumps({
            'Question '  :self.question,
            'Answers '   :self.answers,
            'Origin '    :self.origin,
            'num_answers':self.num_answers
        })
