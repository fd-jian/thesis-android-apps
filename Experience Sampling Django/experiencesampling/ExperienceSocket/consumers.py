# chat/consumers.py
import json
from asgiref.sync import async_to_sync
from channels.generic.websocket import WebsocketConsumer
from .models import Message
import time
from .jobs import registerQuestionnaireSequence


curr_open_rooms = {}

class ChatConsumer(WebsocketConsumer):

    def connect(self):
        self.room_name = self.scope['url_route']['kwargs']['room_name']
        self.room_group_name = 'ExperienceSocket_%s' % self.room_name



        # Join room group
        async_to_sync(self.channel_layer.group_add)(
            self.room_group_name,
            self.channel_name
        )

        self.accept()

        delta_time = 120 * 60
        cur_time = time.time()

        try:
            old_time = curr_open_rooms[self.room_group_name]
            print("Roomname found " + self.room_group_name)

            if cur_time - delta_time > old_time:
                print( self.room_group_name + " is expired")
                curr_open_rooms[self.room_group_name] = cur_time
                registerQuestionnaireSequence(self.room_group_name)
            else:
                print( self.room_group_name + " is not expired")
                curr_open_rooms[self.room_group_name] = cur_time

        except:
            print("Registered new Group")
            curr_open_rooms[self.room_group_name] = cur_time
            registerQuestionnaireSequence(self.room_group_name)



    def disconnect(self, close_code):
        # Leave room group
        async_to_sync(self.channel_layer.group_discard)(
            self.room_group_name,
            self.channel_name
        )


    # Receive message from WebSocket
    def receive(self, text_data):
        text_data_json = json.loads(text_data)
        message = text_data_json['message']

        self.add_message_to_database(message)

        # Send message to room group
        async_to_sync(self.channel_layer.group_send)(
            self.room_group_name,
            {
                'type': 'chat_message',
                'message': message
            }
        )

    # Receive message from room group
    def chat_message(self, event):
        message = event['message']

        # Send message to WebSocket
        self.send(text_data=json.dumps({
            'message': message
        }))


    def add_message_to_database(self, message):

        to_save = Message.objects.create(content=message)
        print("Added message: " + to_save.content + " to database at " + str(to_save.timestamp))

    def messages_to_json(self, messages):
        result = []
        for message in messages:
            result.append(self.message_to_json(message))
        return result


    def message_to_json(self, message):
        return {
            'content' : message.content,
            'timestamp' : str(message.timestamp)
        }

