# Explorateur 3D Mandelbrot/Julia tricomplexe (Java)

Application Java Swing qui réalise un rendu par **lancement de rayons / ray marching** d'une coupe 3D d'ensembles remplis tricomplexes associés à:

\[
 f(z) = z^p + c
\]

Le rendu est basé sur un test d'appartenance volumique (ensemble rempli) et un parcours de rayon avec pas `epsilon` configurable, dans l'esprit de la technique discutée dans l'article: https://arxiv.org/pdf/1811.09697.

## Fonctionnalités

- Rendu 3D d'une coupe de l'ensemble de **Mandelbrot** ou de **Julia** tricomplexe.
- Navigation interactive:
  - rotation (glisser bouton gauche),
  - déplacement/pan (glisser bouton droit ou Shift+glisser),
  - zoom (roulette souris).
- Sélection de **3 unités distinctes** parmi 8 bases tricomplexes pour définir la coupe 3D.
- Réglage de `epsilon` pour la marche le long du rayon.
- Réglage de `p`, du nombre d'itérations, du bailout et du nombre max de pas de rayon.

## Base tricomplexe utilisée

Les 8 composantes sont:

1. `1`
2. `i1`
3. `i2`
4. `i1i2`
5. `i3`
6. `i1i3`
7. `i2i3`
8. `i1i2i3`

avec `i1^2 = i2^2 = i3^2 = -1` (unités commutatives).

## Lancer

Prérequis: Java 17+

```bash
mvn compile
mvn exec:java -Dexec.mainClass=fractal3d.FractalViewerApp
```

Si `exec-maven-plugin` n'est pas présent localement, vous pouvez aussi lancer directement:

```bash
javac -d out src/main/java/fractal3d/*.java
java -cp out fractal3d.FractalViewerApp
```
