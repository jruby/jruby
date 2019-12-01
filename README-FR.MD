# JRuby - une implémentation du langage Ruby sur la JVM

Master: [![Build Status](https://travis-ci.org/jruby/jruby.svg?branch=master)](https://travis-ci.org/jruby/jruby)
9.1 branche: [![Build Status](https://travis-ci.org/jruby/jruby.svg?branch=jruby-9.1)](https://travis-ci.org/jruby/jruby/branches)

## A propos

JRuby est une implementation du [langage Ruby](http://www.ruby-lang.org)
en utilisant la JVM (Java Virtual Machine).

L'objectif est de faire une implémentation complète, fonctionnelle et rapide de Ruby, tout en fournissant de nouvelles fonctionnalitées comme la concurrence (programmation concurentielle) sans un
[GIL(global-interpreter-lock)](http://en.wikipedia.org/wiki/Global_Interpreter_Lock),
un véritable parallèlisme, une intégration au langage Java afin de permettre l'utilisation des classes Java dans un programme Ruby et de permettre à JRuby d'être embarqué dans une application Java.
Vous pouvez utiliser Jruby simplement comme une version plus rapide de Ruby, pour utiliser Ruby tournant sur la JVM et de pouvoir utiliser les librairies pour la concurrence, pour embarquer Ruby en tant que langage de script dans un programme Java, ou plein d'autres possibilitées.

Nous sommes une communautée accueillante - vous pouvez nous parlez sur [#jruby sur Freenode](http://richard.esplins.org/siwi/2011/07/08/getting-started-freenode-irc/).
Des membres de l'équipe principale sont à la fois en Europe et aux USA, donc couvrant une bonne partie de la journée.

Visitez le [site JRuby](https://www.jruby.org/) et le [wiki JRuby](https://github.com/jruby/jruby/wiki) pour plus d'informations.

## Installer JRuby

Pour lancer JRuby vous allez avoir besoir d'un JRE (Java VM runtime environment) de version 8 ou supérieure.

Votre système d'exploitation peut fournir un JRE et JRuby dans un gestionnaire de paquets, mais ne vous trouverez cela que dans une vieille version.

Une solution alternative est d'utiliser une des version du [gestionnaire de versions Ruby](https://www.ruby-lang.org/en/documentation/installation/#managers).

Pour [`rbenv`](https://github.com/sstephenson/rbenv) vous allez avoir besoin du
[`ruby-build`](https://github.com/sstephenson/ruby-build) plugin. Votre gestionnaire système les fournit déjà peut être. Pour vérifier quelles versions de JRuby sont disponibles, vous devez faire:

```
$ rbenv install jruby
```

Note: effectuez regulièrement 'git update rbenv' ou la liste pourrait ne pas être à jour.

Nous vous recommandons de prendre la version la plus à jour de la liste. 
Vous pouvez installez cette version (9.1.17.0 juste pour illustration):


```
$ rbenv install jruby-9.1.17.0
```

Pour [`rvm`](https://rvm.io) faites simplement:

```
$ rvm install jruby
```

Vous pouvez aussi utiliser [`Homebrew`](https://brew.sh/):

```
$ brew install jruby
```

Vous pouvez aussi [télécharger directement à partir du site JRuby ](https://www.jruby.org/download) que vous pouvez ensuite décompresser et lancer.

## Assembler JRuby

Regarder le [BUILDING](BUILDING.md) pour plus d'informations, comment compiler JRuby et comment le tester.

## Auteurs

Stefan Matthias Aust, Anders Bengtsson, Geert Bevin, Ola Bini,
 Piergiuliano Bossi, Johannes Brodwall, Rocky Burt, Paul Butcher,
 Benoit Cerrina, Wyss Clemens, David Corbin, Benoit Daloze, Thomas E Enebo,
 Robert Feldt, Chad Fowler, Russ Freeman, Joey Gibson, Kiel Hodges,
 Xandy Johnson, Kelvin Liu, Kevin Menard, Alan Moore, Akinori Musha,
 Charles Nutter, Takashi Okamoto, Jan Arne Petersen, Tobias Reif, David Saff,
 Subramanya Sastry, Chris Seaton, Nick Sieger, Ed Sinjiashvili, Vladimir Sizikov,
 Daiki Ueno, Matthias Veit, Jason Voegele, Sergey Yevtushenko, Robert Yokota,
   and many gracious contributors from the community.

JRuby utilises du code généreusement partager par le créateur du langage Ruby,
Yukihiro Matsumoto <matz@netlab.co.jp>.

Contact projet: Thomas E Enebo <tom.enebo@gmail.com>

## Licence

JRuby vous est licencié sous 3 licences - EPL 2.0, GPL 2 et LGPL 2.1.
Certains composents ont d'autres licences ou droits d'auteurs. Regardez le fichier [COPYING](COPYING)
pour plus de précisions.
