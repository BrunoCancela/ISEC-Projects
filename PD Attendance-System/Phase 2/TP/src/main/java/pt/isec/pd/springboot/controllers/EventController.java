package pt.isec.pd.springboot.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pt.isec.pd.springboot.m1.commands.ComandoStruct;
import pt.isec.pd.springboot.m1.commands.ProcessAdminCommands;
import pt.isec.pd.springboot.m1.database.DBConnection;
import pt.isec.pd.springboot.m1.server.ServidorPrincipal;
import pt.isec.pd.springboot.models.EventConfig;

import java.sql.Connection;
import java.sql.SQLException;

@RestController
@RequestMapping("/eventos")
public class EventController {

    private final ServidorPrincipal servidorPrincipal;

    @Autowired
    public EventController(ServidorPrincipal servidorPrincipal) {
        this.servidorPrincipal = servidorPrincipal;
    }

    @PostMapping
    public ResponseEntity<Object> createEvent(@RequestBody EventConfig eventConfig, Authentication authentication) throws SQLException {

        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);
        ComandoStruct comando =  new ComandoStruct();
        comando.setEvent(eventConfig.getName());
        comando.setLocal(eventConfig.getLocation());
        comando.setData(eventConfig.getDate());
        comando.setBeginHour(eventConfig.getStartTime());
        comando.setEndHour(eventConfig.getEndTime());
        comando.setEmail(authentication.getName());
        String res = ProcessAdminCommands.createEvent(dbConnection,comando);

        dbConnection.close();
        if (res.contains("sucesso")){
            servidorPrincipal.onDatabaseUpdate();
        }

        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity deleteEvent(@PathVariable("name") String name) throws SQLException {
        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);
        ComandoStruct comando =  new ComandoStruct();
        comando.setEvent(name);
        String res = ProcessAdminCommands.removeEvent(dbConnection,comando);

        dbConnection.close();
        if(res.contains("sucesso")){
            servidorPrincipal.onDatabaseUpdate();
        }

        return ResponseEntity.ok(res);
    }

    @GetMapping("/{nomeEvento}/presencas")
    public ResponseEntity<?> viewAttendances(@PathVariable String nomeEvento) throws SQLException {
        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);
        ComandoStruct comando =  new ComandoStruct();
        comando.setEvent(nomeEvento);
        String res = ProcessAdminCommands.checkEventAttendance(dbConnection,comando);
        dbConnection.close();
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{nomeEvento}/gerarcodigo")
    public ResponseEntity<Object> generateCode(@PathVariable String nomeEvento,
                                               @RequestParam(value="min", required=false) Integer min) throws SQLException {

        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);
        ComandoStruct comando = new ComandoStruct();
        System.out.println(nomeEvento);
        System.out.println(min);
        comando.setEvent(nomeEvento);
        comando.setPeriod(String.valueOf(min));
        String res = ProcessAdminCommands.generateEventCode(dbConnection,comando);

        dbConnection.close();
        if(res.contains("sucesso")){
            servidorPrincipal.onDatabaseUpdate();
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity getAllEvents() throws SQLException {
        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);
        ComandoStruct comando = new ComandoStruct();
        comando.setEvent("");
        comando.setBeginHour("");
        comando.setEndHour("");
        String res = ProcessAdminCommands.listEvents(dbConnection,comando);

        dbConnection.close();

        return ResponseEntity.ok(res);
    }

    // Métodos adicionais conforme necessário...
}