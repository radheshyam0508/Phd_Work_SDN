#!/bin/bash

end=5
devname=$1-'wlan0'

while [ $end -ge 5 ]; do
    iw dev $devname connect wifi
    echo "5 seconds has elapsed."
    sleep 5
    end=$((SECONDS+5))
done

