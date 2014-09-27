epursuit
========

**Note:** This project was created in a very tight time frame 4 years ago and simply converted vom google code to github for nostalgic reasons.

ePursuit is a "Real Life Scotland Yard" telephone bot and was created for the purpose of introducing freshmen to their new location.

It connects to Asterisk and sets up a telephone bot which is capable of calling all Mr X, recording where they are and sending these recordings to the detectives.

It is very easy to configure once you have Asterisk configured with the SIP provider of your choice.

Quickstart
----------

1. Download and unzip ePursuit
2. Put Mr X and Agent numbers (SIP/.../Number) - one number per line - into files
3. Edit the epursuit.properties, to fit your Asterisk system (remember to record some sounds - gsm is the default format)
4. Start ePursuit: java -jar epursuit.jar /PATH/TO/epursuit.properties

Commands
--------

| Command       | Description                                                                                   |
|---------------|-----------------------------------------------------------------------------------------------|
| call mrx      | starts calling all Mr X                                                                       |
| call test     | calls the testnumber specified in the config and plays the same stuff, the agents get to hear |
| call agents   | starts calling all agents (only use it after you have at least once called Mr X)              |
| reload mrx    | reloads Mr X list                                                                             |
| reload agents | reloads Agent list                                                                            |
| quit          | guess what                                                                                    |
