from lxml import etree
from Point import Point
from Oval import Oval
from Ellipse import Ellipse
from Circle import Circle
from Rectangle import Rectangle
from Square import Square
from Polygon import Polygon
from GenerateXMLDocument import generateXMLDocument

"""
Este metodo genera el xml resultante a partir de las regiones que tenemos en el programa.
Deben ser todas del mismo tipo, de no ser asi obtendremos una excepcion que nos lo dira.
Cuando le pasamos las regiones lo que hacemos es ir aniadiendolas al elemento ROIs que creamos en el mismo metodo.
De esta forma cuando lo invoquemos lo podremos aniadir directamente a la raiz del archivo XML que estamos generando.
"""

ROIsofImage=etree.Element("ROIsofImage")
doc=etree.ElementTree(ROIsofImage)
image=etree.Element("image")
image.append(etree.Element("path"))
image.append(etree.Element("height"))
image.append(etree.Element("width"))
image[0].text="prueba" #Introducimos el nombre de la imagen sobre la que vamos a trabajar
image[1].text="1500" #Introducimos el alto de la imagen sobre la que vamos a trabajar
image[2].text="2600" #Introducimos el ancho de la imagen sobre la que vamos a trabajar
ROIsofImage.append(image)

"""
PRUEBAS
Se ha comprobado que se pueden crear todas las regiones
#Generamos en la clase el xml y luego lo devolvemos para guardarlo en el archivo final
punto = Point('59958','6557')
ROIs.append(punto.generateXML())

oval = Oval('5','6','7','8')
ROIs.append(oval.generateXML())

ellipse = Ellipse('7','8','0.2')
ROIs.append(ellipse.generateXML())

circle = Circle('40','50','40000000')
ROIs.append(circle.generateXML())

rectangle = Rectangle('5','8','88','99')
ROIs.append(rectangle.generateXML())

square = Square('55','99','520')
ROIs.append(square.generateXML())

xs=[1,2,3,4,5,6]
ys=[6,5,4,555,2,3]

polygon = Polygon(xs,ys)
ROIs.append(polygon.generateXML())
"""

#Como hemos comentado en el metodo, aqui solo tenemos que llamarlo y el se encarga de generar el xml esperado. Solo queda aniadirlo a la raiz
rois = [Ellipse('7','8','0.2'),Ellipse('55','55','0.222'),Ellipse('777','777','0.777')]
ROIsofImage.append(generateXMLDocument(rois))

"""
En esta parte es donde creamos el archivo XML que tiene de contenido las regiones que hemos captado de OpenCV.
Al abrir ese archivo, si no esta se crea pero si ya esta generado se machaca el contenido que tuviera, de esta forma es conveniente
que el nombre del archivo sea diferente cada vez que queramos guardar las regiones de una foto.

"""
archi = open('datos.xml', 'w')
archi.flush()
archi.write(etree.tostring(ROIsofImage,pretty_print=True ,xml_declaration=True, encoding="utf-8"))

archi.close()