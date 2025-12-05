#!/bin/bash

main() {
    validChoice=0

    while [ ${validChoice} -eq 0 ]
    do
        echo "Choisissez:"
        echo "(1) Brouiller l'image"
        echo "(2) Débrouiller l'image"
        echo "(3) Casser la clé"
        echo "(4) exit"
        echo
        read -r process

        if isNumber "${process}" && [ "${process}" -eq 1 -o "${process}" -eq 2 -o "${process}" -eq 3 -o "${process}" -eq 4 ]
        then
            validChoice=1
        fi
    done
    if [ "${process}" -eq 1 ]
    then
        scramble
    elif [ "${process}" -eq 2 ]
    then
        unscramble
    elif [ "${process}" -eq 3 ]
    then
        breakKey
    else
        exit
    fi
}

scramble() {
    key=$(getKey)
    inPath=$(getInPath)
    outPath=$(getOutPath)
    if [ ! -e "./src/Brouillimg.class" ]
    then
        javac ./src/Brouillimg.java
    fi
    java -cp ./src Brouillimg scramble "${inPath}" "${key}" "${outPath}"
    xdg-open "${outPath}"
}

unscramble() {
    if [ "$1" = "break" ]
    then
        key=$2
        inPath=$3
    else
        key=$(getKey)
        inPath=$(getInPath)
    fi
    if [ ! -e "./src/Brouillimg.class" ]
    then
        javac ./src/Brouillimg.java
    fi
    java -cp ./src Brouillimg unscramble "${inPath}" "${key}" "${inPath%.png}"-unscrambled.png
    xdg-open "${inPath%.png}"-unscrambled.png
}

breakKey() {
    inPath=$(getInPath)
    method=$(getBreakMethod)
    if [ ! -e "./src/Brouillimg.class" ]
    then
        javac ./src/Brouillimg.java
    fi
    key=$(java -cp ./src Brouillimg "${method}" "${inPath}" 0 | tail -n 1 | cut -d: -f 2 | tr -d '[:space:]')
    echo "La clé trouvé est: ${key}"
    unscramble break "${key}" "${inPath}" > /dev/null

}

isNumber() {
	[ "$1" -eq 1 ] 2> /dev/null
	[ $? -eq 0 -o $? -eq 1 ]
	return $?
}

getBreakMethod() {
    valid=0

    while [ "${valid}" -eq 0 ]
    do
        echo "Méthode:" >&2
        echo "1) Distance euclidienne" >&2
        echo "2) Corrélation de Pearson" >&2
        read -r method

        if isNumber "${method}" && [ "${method}" -eq 1 -o "${method}" -eq 2 ]
        then
            valid=1
        fi
    done

    if [ "${method}" -eq 1 ]
    then
        echo "euclidean"
    else
        echo "pearson"
    fi
}

getKey() {
    valid=0

    while [ "${valid}" -eq 0 ]
    do
        read -r -p "Entrez clé: " key

        if isNumber "${key}" && [ "${key}" -ge 0 ]
        then
            valid=1
        fi
    done

    echo "${key}"
}

getInPath() {
    valid=0

    while [ "${valid}" -eq 0 ]
    do
        read -e -r -p "Entrez le chemin de l'image d'entrée: " inPath

        if ! isNumber "${inPath}" && [ -f "${inPath}" ]
        then
            valid=1
        else
            echo "Fichier invalide" >&2
        fi
    done

    echo "${inPath}"
}

getOutPath() {
    valid=0

    while [ "${valid}" -eq 0 ]
    do
        read -e -r -p "Entrez le chemin de l'image de sortie: " outPath

        if ! isNumber "${outPath}" && [ -e "${outPath}" ]
        then
            if [ -d "${outPath}" ]
            then 
                outPath="${outPath%/}/out.png"
            fi
            valid=1
        else
            echo "chemin invalide" >&2
        fi
    done

    echo "${outPath}"
}

while true
do
    main
    echo
done
