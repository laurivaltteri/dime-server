GRADLE   = ./gradlew -q
TARGET   = build/libs/dime-server.jar
JAVADOC_DIR = build/docs/javadoc/
JAVADOC_WEB = shell.hiit.fi:/group/reknow/public_html/javadoc/dime-server/
SOURCES := $(shell find src/ -name '[A-Z]*.java' -or -name '*.html')

DIME_HOME = ~/.dime

DOCKER_DIR = $(DIME_HOME)/docker
DB_FILE = $(DIME_HOME)/database/h2.mv.db
AUTOGEN_TMP_DIR = $(DIME_HOME)/tmp/autogen

LOG_HEAD = "[MAKE]"

all:	assemble

assemble:  $(TARGET)

$(TARGET): $(SOURCES)
	$(GRADLE) assemble

run:    $(TARGET)
	java -jar $(TARGET)

test:
	rm -rf ./build/reports/tests/
	$(GRADLE) test
	@echo $(LOG_HEAD) Now open ./build/reports/tests/index.html

clean:
	$(GRADLE) clean

doc: $(SOURCES)
	$(GRADLE) javadoc
	chmod -R a+r $(JAVADOC_DIR)
	rsync -var $(JAVADOC_DIR) $(JAVADOC_WEB)
	@echo
	@echo $(LOG_HEAD) Now open ./build/docs/javadoc/index.html

docker: $(TARGET)
	docker build -t dime-server .

docker_dir:
	@echo $(LOG_HEAD) Creating directory for database ...
	mkdir -p $(DOCKER_DIR)
	chmod 777 $(DOCKER_DIR)

runDocker: docker docker_dir
	docker run -it -p 8080:8080 -v $(DOCKER_DIR):/var/lib/dime dime-server

autogenDb:
	@echo $(LOG_HEAD) Creating temporary database auto-generated by hibernate
	rm -rf $(AUTOGEN_TMP_DIR)
	@echo $(LOG_HEAD) Running tests to auto-generate database
	SPRING_PROFILES_ACTIVE=autogen $(MAKE) test

initSchema: autogenDb
	rm -f src/main/resources/db/changelog/db.changelog-master.xml
	$(GRADLE) generateChangelog

updateSchema: $(DB_FILE) autogenDb 
	$(GRADLE) diffChangeLog

install: 
	./install-dime.sh
