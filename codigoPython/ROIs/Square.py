from lxml import etree
from Rectangle import Rectangle


class Square(Rectangle):

    def __init__(self,x,y,side):
        Rectangle.__init__(self,x,y,side,side)


    def getSide(self):
        return self.getHeight()


    def generateXML(self):
        square = etree.Element("square")
        point = etree.Element("point")
        point.append(etree.Element("x"))
        point.append(etree.Element("y"))
        point[0].text = str(self.getX())
        point[1].text = str(self.getY())
        square.append(point)
        square.append(etree.Element("side"))
        square[1].text = str(self.getSide())
        return square


