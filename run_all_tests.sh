#!/usr/bin/env bash

sbt clean scalastyleAll compile A11y/test coverage test coverageOff coverageReport dependencyUpdates

