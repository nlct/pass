all	: passeditor-en.pdf

../lib/manual	:
		 mkdir -p ../lib/manual

../lib/manual/en	:
		 mkdir -p ../lib/manual/en

../lib/manual/en/images	: ../lib/manual/en
		mkdir -p ../lib/manual/en/images

passeditor-en.pdf	: passeditor-en.tex passeditor.cls
			arara passeditor-en

passeditor-en.tex	: createpasseditordocs passeditor-en.xml \
			../lib/manual/en/images \
			../lib/dictionary/passeditor-en.xml \
			../java/PassEditor.java
			createpasseditordocs en
			cp -r images-en/*.png ../lib/manual/en/images/
			rm -r -f ../lib/manual/en/JavaHelpSearch 
			cd ../lib/manual/en; \
			jh2indexer -locale en .

clean	:
	rm -f passeditor-*.aux
	rm -f passeditor-*.glg
	rm -f passeditor-*.glstex
	rm -f passeditor-*.log
	rm -f passeditor-*.out
	rm -f passeditor-*.toc
	rm -f passeditor-*.lof
	rm -f passeditor-*.lot
	rm -f passeditor-*.ldf
	rm -f passeditor-*-boilerplate.tex

squeaky	:
	rm -f passeditor-en.tex
	rm -f passeditor-en.bib
	rm -f passeditor-en.pdf
	rm -f -r ../lib/manual/en

