package com.mediturno.backend.documentos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.mediturno.backend.ordenes.model.Orden;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;

/**
 * Generación de documentos (por ahora, el PDF de la orden, CU-16). Aparece en el diagrama de secuencia de CU-16 como
 * "DocumentosService" (Apache PDFBox); no figura en el diagrama de clases.
 */
@Service
public class DocumentosService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * CU-16. PDF de la orden: número, fecha de emisión, paciente, médico, especialidad, fecha y horario, consultorio, número de
     * la reserva y hora estimada de atención. Hay que llamarlo con la orden cargada (sus relaciones se leen acá), dentro de una
     * transacción.
     *
     * @param horaEstimada hora estimada de atención del número de la reserva (la calcula quien llama: depende del turno)
     */
    public byte[] generarPDF(Orden orden, java.time.LocalTime horaEstimada) {
        OcurrenciaTurno ocurrencia = orden.getReserva().getOcurrenciaTurno();
        TurnoSemanal turno = ocurrencia.getTurnoSemanal();
        List<String[]> lineas = new ArrayList<>();
        lineas.add(new String[] {"Número de orden", String.valueOf(orden.getNumeroOrden())});
        lineas.add(new String[] {"Emitida", orden.getFechaEmision().format(FECHA_HORA)});
        lineas.add(new String[] {"Paciente", orden.getPaciente().getApellido() + ", " + orden.getPaciente().getNombre()});
        lineas.add(new String[] {"Cédula", orden.getPaciente().getCedula()});
        lineas.add(new String[] {"Médico", turno.getMedico().getApellido() + ", " + turno.getMedico().getNombre()});
        lineas.add(new String[] {"Especialidad", turno.getEspecialidad().getNombre()});
        lineas.add(new String[] {"Fecha", ocurrencia.getFecha().format(FECHA)});
        lineas.add(new String[] {"Horario del turno", ocurrencia.getHoraInicio().format(HORA) + " a " + ocurrencia.getHoraFin().format(HORA)});
        lineas.add(new String[] {"Consultorio", turno.getConsultorio().getNombre()});
        lineas.add(new String[] {"Número", String.valueOf(orden.getReserva().getNumero())});
        lineas.add(new String[] {"Hora estimada de atención", "~" + horaEstimada.format(HORA)});

        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float x = 60;
            float y = pagina.getMediaBox().getHeight() - 80;
            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                escribir(contenido, negrita, 22, x, y, "MeDiTurno");
                y -= 26;
                escribir(contenido, normal, 14, x, y, "Orden de consulta");
                y -= 40;
                for (String[] linea : lineas) {
                    escribir(contenido, negrita, 12, x, y, linea[0] + ":");
                    escribir(contenido, normal, 12, x + 190, y, linea[1]);
                    y -= 24;
                }
                y -= 16;
                escribir(contenido, normal, 10, x, y, "La hora de atención es estimada. Presente esta orden en el consultorio.");
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la orden " + orden.getNumeroOrden(), e);
        }
    }

    private static void escribir(PDPageContentStream contenido, PDType1Font fuente, float tamano, float x, float y, String texto)
            throws IOException {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(texto);
        contenido.endText();
    }
}
