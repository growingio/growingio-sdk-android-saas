#!/bin/bash

declare popFile='./gradle.properties'
declare popImp='./popImp'
if [ ! -f "$popImp" ];then
    touch "$popImp"
fi

while read line  
do  
if [[ $line == *'ISOP'* ]]
then
    echo 'ISOP=true'>>$popImp
else
    echo $line>>$popImp
fi
done < $popFile  
rm  $popFile
mv  $popImp  $popFile
