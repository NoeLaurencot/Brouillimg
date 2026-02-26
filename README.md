# Brouillimg

Projet de SAÉ 1-2 - BUT Informatique S1-B2  
Auteurs : LAMOUR Pierre, LAURENÇOT Noé

## Présentation

Brouillimg est un programme Java permettant de brouiller et de débrouiller des images en permutant leurs lignes à l'aide d'une clé. Il intègre également des fonctionnalités capables de retrouver la clé utilisée pour brouiller une image, sans la connaître à l'avance, grâce à différents critères.

### Fonctionnement

La clé est un entier de **15 bits** (0 – 32 767), décomposé en deux parties :
- **bits 14–7** → offset `r` (8 bits)
- **bits 6–0**  → step `s` (7 bits)

La position de la ligne `i` dans l'image brouillée est calculée par :

$$\text{pos}(i) = (r + (2s + 1) \times i) \mod \text{hauteur}$$

## Compilation

Depuis la racine du projet :

```bash
mkdir -p out
javac -d out src/Profiler.java src/Brouillimg.java
```

## Utilisation

```
java -cp out Brouillimg <processus> <image_entrée> <clé> [image_sortie]
```

| Paramètre       | Description                                                        |
|-----------------|--------------------------------------------------------------------|
| `<processus>`   | Action à effectuer (voir tableau ci-dessous)                       |
| `<image_entrée>`| Chemin vers l'image PNG d'entrée                                   |
| `<clé>`         | Clé codée sur 15 bits (0 - 32 767)                                 |
| `[image_sortie]`| *(Optionnel)* Chemin de l'image de sortie (par défaut "out.png")   |

### Processus

| Processus    | Description                                                                  |
|--------------|------------------------------------------------------------------------------|
| `scramble`   | Brouille l'image en permutant ses lignes selon la clé fournie                |
| `unscramble` | Débrouille l'image en inversant la permutation grâce à la clé fournie        |
| `euclidean`  | Casse la clé par la distance euclidienne entre lignes                        |
| `pearson`    | Casse la clé par la corrélation de Pearson                                   |
| `variance`   | Casse la clé en maximisant la variance entre lignes (En réalité Manhattan)   |
| `neighbor`   | Casse la clé par similarité entre lignes voisines                            |
| `profile`    | Mode interactif pour comparer les performances des méthodes de cassage       |

## Exemples

### Brouiller une image

```bash
java -cp out Brouillimg scramble images/paysage.png 1234 images/paysage_brouille.png
```

### Débrouiller

```bash
java -cp out Brouillimg unscramble images/paysage_brouille.png 1234 images/paysage_restaure.png
```

### Casser

```bash
java -cp out Brouillimg euclidean images/paysage_brouille.png 0 images/paysage_restaure.png
java -cp out Brouillimg pearson   images/paysage_brouille.png 0 images/paysage_restaure.png
java -cp out Brouillimg variance  images/paysage_brouille.png 0 images/paysage_restaure.png
java -cp out Brouillimg neighbor  images/paysage_brouille.png 0 images/paysage_restaure.png
```

> **Note :** Pour les processus de cassage, la valeur de <clé> est ignorée (mettre 0).

### Profiler les méthodes de cassage

```bash
java -cp out Brouillimg profile images/paysage_brouille.png 0
```

Le programme propose un menu interactif pour choisir la méthode à évaluer et affiche les temps d'exécution.
