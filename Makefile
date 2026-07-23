VERSION_BUILD := $(shell git rev-parse --short HEAD)

RELEASE_APK := app/build/outputs/apk/release/app-release.apk
OUTPUT_APK := app/build/outputs/apk/release/CardTracker-$(VERSION_BUILD).apk

.PHONY: build clean test

build: clean
	gradle assembleRelease
	mv $(RELEASE_APK) $(OUTPUT_APK)

clean:
	gradle clean

test:
	timeout 900 gradle test
