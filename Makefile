build:
	docker build . -t kcsv

test: build
	docker run kcsv ./gradlew test