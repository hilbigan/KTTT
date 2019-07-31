#!/bin/bash
while true; do
	java -jar 'selfplay-1.0.jar'	
	sleep 1
	cd models
	python3 keras-train.py
	cd ..
	sleep 1
	date >> log
done
