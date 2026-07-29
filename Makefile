VERSION_BUILD := $(shell git rev-parse --short HEAD)

RELEASE_APK := app/build/outputs/apk/release/app-release.apk
OUTPUT_APK := app/build/outputs/apk/release/CardTracker-$(VERSION_BUILD).apk

.PHONY: build clean test

build: clean
	gradle assembleRelease
	echo -e "\033[0;32mRenaming the APK\033[0m"
	mv $(RELEASE_APK) $(OUTPUT_APK)

clean:
	gradle clean

test:
	timeout 900 gradle test
