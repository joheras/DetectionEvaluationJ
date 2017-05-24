from lxml import etree
from ROI import ROI


class Ellipse(ROI):

    def __init__(self,x,y,aspRat):
        ROI.__init__(self,x,y)
        self.aspectRatio=aspRat


    def getAspectRatio(self):
        return self.aspectRatio


    def generateXML(self):
        ellipse = etree.Element("ellipse")
        point = etree.Element("point")
        point.append(etree.Element("x"))
        point.append(etree.Element("y"))
        point[0].text = str(self.getX())
        point[1].text = str(self.getY())
        ellipse.append(point)
        ellipse.append(etree.Element("aspectRatio"))
        ellipse[1].text = str(self.getAspectRatio())
        return ellipse


