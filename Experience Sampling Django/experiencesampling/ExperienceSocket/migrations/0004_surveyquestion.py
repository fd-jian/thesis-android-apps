# Generated by Django 2.2.7 on 2020-07-16 16:36

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('ExperienceSocket', '0003_auto_20200716_1822'),
    ]

    operations = [
        migrations.CreateModel(
            name='SurveyQuestion',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('question', models.CharField(help_text='zB: Wie geht es dir?', max_length=256)),
                ('question_type', models.CharField(choices=[('NumAns', 'Numerical Answer'), ('TxtAns', 'Text Answer')], default='TxtAns', max_length=6)),
                ('numerical_answer_lower', models.IntegerField(blank=True, help_text='Lower Bound', null=True)),
                ('numerical_answer_upper', models.IntegerField(blank=True, help_text='Upper Bound', null=True)),
                ('text_answer', models.CharField(blank=True, help_text="zB: 'Ja, Vielleicht, Nein' oder leer lassen falls per Voice geantwortet werden soll", max_length=256, null=True)),
                ('survey', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='ExperienceSocket.Survey')),
            ],
        ),
    ]