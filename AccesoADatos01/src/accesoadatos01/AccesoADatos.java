/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package accesoadatos01;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.text.SimpleDateFormat;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

/**
 *
 * author marta albarracin
 */
public class AccesoADatos {

    // Función para convertir una cadena XML en un documento XML
    public static Document parseXML(String xmlString) throws Exception {
        // Configuración del builder para parsear el XML utilizando DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Se crea una fuente de entrada para el XML a partir de la cadena
        InputSource inputSource = new InputSource(new StringReader(xmlString));

        // Parsear y devolver el documento XML
        return builder.parse(inputSource);
    }

    public static void main(String[] args) {
        try {
            // Ruta del archivo XML dentro del proyecto
            String filePath = "contratos-adjudicados-oct-23.xml";

            // Realiza la solicitud HTTP para obtener el contenido del archivo XML
            String xmlData = obtenerContenidoDesdeArchivo(filePath);

            Document xmlDoc = parseXML(xmlData);
            NodeList contratoList = xmlDoc.getElementsByTagName("Row");

            for (int i = 1; i < contratoList.getLength(); i++) {
                Node contratoNode = contratoList.item(i);

                if (contratoNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element contratoElement = (Element) contratoNode;
                    // Agregamos los datos a sus respectivas columnas utilizando la función de getElementsByTagName()
                    String nif = contratoElement.getElementsByTagName("Data").item(0).getTextContent();
                    String adjudicatario = contratoElement.getElementsByTagName("Data").item(1).getTextContent();
                    String objetoGenerico = contratoElement.getElementsByTagName("Data").item(2).getTextContent();
                    String objeto = contratoElement.getElementsByTagName("Data").item(3).getTextContent();
                    String fechaAdjudicacionStr = contratoElement.getElementsByTagName("Data").item(4).getTextContent();
                    String importeStr = contratoElement.getElementsByTagName("Data").item(5).getTextContent();
                    String proveedoresConsultados = contratoElement.getElementsByTagName("Data").item(6).getTextContent();
                    // "TIPO DE CONTRATO" está en la posición 7, pero lo excluiremos

                    // Convertir fecha a los tipos correspondientes
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    Date parsedDate = dateFormat.parse(fechaAdjudicacionStr);
                    java.sql.Date fechaAdjudicacion = new java.sql.Date(parsedDate.getTime());
                    
                    // Eliminamos todo excepto los dígitos y el punto decimal
                    double importe = Double.parseDouble(importeStr.replaceAll("[^0-9.]", ""));
                    
                    // Insertamos los datos del fichero XML en la base de datos
                    try ( Connection connection = ConexionMySQL.obtenerConexion()) {
                        String insertQuery = "INSERT INTO ContratosMenores (NIF, ADJUDICATARIO, OBJETO_GENERICO, OBJETO, "
                                + "FECHA_ADJUDICACION, IMPORTE, PROVEEDORES_CONSULTADOS) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try ( PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                            preparedStatement.setString(1, nif);
                            preparedStatement.setString(2, adjudicatario);
                            preparedStatement.setString(3, objetoGenerico);
                            preparedStatement.setString(4, objeto);
                            preparedStatement.setDate(5, fechaAdjudicacion);
                            preparedStatement.setDouble(6, importe);
                            preparedStatement.setString(7, proveedoresConsultados);

                            // Ejecutamos la consulta de inserción de datos
                            preparedStatement.executeUpdate();
                        }
                    } catch (SQLException e) {
                        // Manejar excepciones en caso de algún problema durante la inserción en la base de datos
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Datos almacenados en la base de datos correctamente.");

        } catch (Exception e) {
            // Manejar excepciones en caso de algún problema durante la lectura del XML
            e.printStackTrace();
        }
    }

    // Función para obtener el contenido desde un archivo local
    private static String obtenerContenidoDesdeArchivo(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }
}
