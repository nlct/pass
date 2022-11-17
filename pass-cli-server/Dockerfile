FROM debian:latest

LABEL vendor="Dickimaw Books" \
      com.dickimaw-books.pass.version="0.1-beta"

ENV TERM=xterm-256color

WORKDIR /usr/src/app

RUN mkdir -p /usr/src/app/bin \
    && mkdir -p /usr/src/app/lib \
    && mkdir -p /usr/src/app/lib/dictionary \
    && apt-get update && apt-get install -y \
        debconf \
        openjdk-17-jdk \
        g++ \
        gcc \
        locales \
    && sed -i -e "s/# en_GB.UTF-8.*/en_GB.UTF-8 UTF-8/" /etc/locale.gen \
    && dpkg-reconfigure --frontend noninteractive locales \
    && update-locale LANG=en_GB.UTF-8 \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s -v /usr/src/app/bin/pass-cli-server /usr/bin/pass-cli-server

COPY bin/* /usr/src/app/bin/
COPY lib/* /usr/src/app/lib/
COPY lib/dictionary/* /usr/src/app/lib/dictionary/

ENV LANGUAGE=en_GB.UTF-8 \
    LANG=en_GB.UTF-8 \
    LC_MONETARY=en_GB.UTF-8 \ 
    LC_PAPER=en_GB.UTF-8 \ 
    LC_NAME=en_GB.UTF-8 \ 
    LC_ADDRESS=en_GB.UTF-8 \ 
    LC_TELEPHONE=en_GB.UTF-8 \ 
    LC_MEASUREMENT=en_GB.UTF-8 \ 
    LC_IDENTIFICATION=en_GB.UTF-8 \ 
    LC_CTYPE=en_GB.UTF-8 \ 
    LC_NUMERIC=en_GB.UTF-8 \ 
    LC_TIME=en_GB.UTF-8 \ 
    LC_COLLATE=en_GB.UTF-8 \ 
    LC_MESSAGES=C.UTF-8 \ 
    PATH=/usr/local/texlive/default/bin/x86_64-linux:$PATH

CMD [ "bash" ]
