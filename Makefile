MAKEFLAGS += --no-print-directory

VERSION_BUILD := $(shell git rev-parse --short HEAD)
RELEASE_APK := app/build/outputs/apk/release/app-release.apk
OUTPUT_APK := app/build/outputs/apk/release/CardTracker-$(VERSION_BUILD).apk

.PHONY: build clean test

build: clean
	gradle assembleRelease
	@$(MAKE) move_file

clean:
	gradle clean

test:
	timeout 900 gradle test

move_file:
	@mv $(RELEASE_APK) $(OUTPUT_APK) > /dev/null 2>&1 && \
	echo -e "\033[32m✔ Success:\033[0m Created \033[36m$(OUTPUT_APK)\033[0m" || \
	echo -e "\033[31m✘ Error:\033[0m Failed to create \033[36m$(OUTPUT_APK)\033[0m"
