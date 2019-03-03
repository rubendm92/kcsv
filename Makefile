build:
	docker-compose build

test: build
	docker-compose run --rm tests
