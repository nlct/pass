pass-cli.pdf	: pass-cli.1
		man -t ./pass-cli.1 | ps2pdf - pass-cli.pdf

pass-cli.1	: pass-cli.pod
		pod2man -c pass-cli pass-cli.pod pass-cli.1

clean	:
	\rm -f pass-cli.{1,pdf}
