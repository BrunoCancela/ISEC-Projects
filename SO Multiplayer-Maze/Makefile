all: JogoUI motor bot

JogoUI: JogoUI.c jogador_comunication.c
	gcc -o JogoUI JogoUI.c -lncurses jogador_comunication.c -lncurses

motor: motor.c motor_processes.c motor_comunication.c
	gcc -o motor motor.c motor_processes.c motor_comunication.c

bot: bot.c
	gcc -o bot bot.c

clean:
	rm -f JogoUI motor bot *.o
