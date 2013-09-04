Cat2Osm2
========

Cat2Osm2 es una herramienta que permite relacionar los Shapefiles del Catastro español con sus Registros y transformarlos en ficheros con formato OSM para su edición mediante JOSM y posterior inclusión en la base de datos de OpenStreetMap.

Wiki
====

http://wiki.openstreetmap.org/wiki/Cat2Osm2


Forma de uso
============

Es necesrio indicar el directorio donde se encuentren los 4 archivos de catastro TAL CUAL se descargan de la web y para una única población.

   java -jar [-XmxMemoria] cat2osm2.jar [Opciones] / [Directorio]


Ejemplo
=======

   java -jar -Xmx10240M cat2osm2.jar /home/yo/carpetaArchivos -rslt MiPueblo -3d 1 -reg 0 -constru -dbg 1 


Parámetros opcionales
=====================

-v            Muestra la version de Cat2Osm2

-rslt         Nombre del resultado (si no se indica, será 'Resultado')

-3d           Exportar las alturas de los edificios (1=Pisos sobre tierra, 0=No, -1=Pisos sobre y bajo tierra), por defecto es 0

-reg          Utilizar un único tipo de registro de catastro (11,14,15 o 17), por defecto es 0=todos

-dbg          Añadir a las geometrías el ID que tienen internamente en Cat2Osm2 para debuggin (1=Si, 0=No), por defecto es 0

-constru      Generar un archivo SOLO con las geometrías CONSTRU

-ejes         Generar un archivo SOLO con las geometrías EJES

-elemlin      Generar un archivo SOLO con las geometrías ELEMLIN

-elempun      Generar un archivo SOLO con las geometrías ELEMPUN

-elemtex      Generar un archivo SOLO con las geometrías ELEMTEX y mostrando todos los textos de Parajes y Comarcas, Información urbana y rústica y Vegetación y Accidentes demográficos

-masa         Generar un archivo SOLO con las geometrías MASA

-parcela      Generar un archivo SOLO con las geometrías PARCELA

-subparce     Generar un archivo SOLO con las geometrías SUBPARCE

-usos         Generar un archivo SOLO con los usos de inmuebles que no se pueden asignar directamente a una construcción