#!/bin/bash
FILE=/services/leader_file.txt

function start_UCSDht() {
  echo "Starting UCSDht ..."
  java -jar /apps/UCSDht/UCSDht.jar &

}

function stop_UCSDht() {

  cont=0
  while pgrep -f UCSDht > /dev/null 2>&1; do

    force=""

    if [ $cont -gt 9 ]; then
      echo "Forcing kill UCSDht "
      force="-9"
    fi

    ppid=$(ps | grep UCSDht | awk '{print $1}' | head -n 1)

    kill $force $ppid

    if [ $? -eq 0 ]; then  # il processo esisteva ed è stato killato
      echo "Killed UCSDht "
    fi

    echo "Waiting for UCSDht death..."

    sleep 1
    cont=$((cont+1))
  done

}


function check() {

  if [[ -f $FILE ]] ; then
     # file exists
     if ! pgrep -f UCSDht > /dev/null 2>&1 ; then
       start_UCSDht
     fi
  else
     # file does not exist
     stop_UCSDht
  fi
}

trap "echo 'Received Signal SIGUSR1'; check" SIGUSR1

while true;
  do
   check
   sleep 5 &
   wait $!
done