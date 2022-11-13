pass-checker.pdf	: pass-checker.1
		man -t ./pass-checker.1 | ps2pdf - pass-checker.pdf

pass-checker.1	: pass-checker.pod
		pod2man -c pass-checker pass-checker.pod pass-checker.1

clean	:
	\rm -f pass-checker.{1,pdf}
