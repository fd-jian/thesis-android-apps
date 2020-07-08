FROM python:3.5

ENV WEBAPP_DIR=/experiencesampling

RUN mkdir $WEBAPP_DIR
WORKDIR $WEBAPP_DIR

ADD . $WEBAPP_DIR/
RUN pip install -r requirements.txt
