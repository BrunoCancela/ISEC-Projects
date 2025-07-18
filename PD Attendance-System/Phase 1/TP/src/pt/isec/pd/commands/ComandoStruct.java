package pt.isec.pd.commands;

import java.io.Serializable;

public class ComandoStruct implements Serializable{

    public enum ComandoType {
        REGISTER_USER,
        GET_USER_INFO,
        UPDATE_USER_INFO,
        REGISTER_ATTENDANCE,
        CHECK_ATTENDANCES,
        CREATE_EVENT,
        EDIT_EVENT,
        REMOVE_EVENT,
        CHECK_CREATED_EVENTS,
        GENERATE_NEW_CODE_EVENT,
        CHECK_EVENT_ATTENDANCE,
        CHECK_USER_ATTENDANCE_TO_ALL_EVENTS,
        REMOVE_USER_ATTENDANCE,
        INSERT_USER_ATTENDANCE,
        USER_LOGIN,
        USER_LOGOUT
    }

    private ComandoType comando;
    private String email;
    private String password;
    private String user;
    private String event;
    private String period;
    private String local;
    private String newEmail;
    private long id;
    private String data;
    private String beginHour;
    private String endHour;
    private String eventCode;
    private String filter;
    private String newEventName;
    private String newEventLocation;
    private String newEventDate;
    private String newEventBeginHour;
    private String newEventEndHour;
    private String date; // Para armazenar a nova data do evento

    // Constructors
    public ComandoStruct() {
        // Default constructor
    }

    // Getters and setters
    public ComandoType getComando() {
        return comando;
    }

    public void setComando(ComandoType comando) {
        this.comando = comando;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getBeginHour() {
        return beginHour;
    }

    public void setBeginHour(String beginHour) {
        this.beginHour = beginHour;
    }

    public String getEndHour() {
        return endHour;
    }

    public void setEndHour(String endHour) {
        this.endHour = endHour;
    }
    public String getEvent() {
        return event;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    public String getLocal() {
        return local;}

    public void setLocal(String local) {
        this.local = local;
    }
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public long getID(){
        return id;
    }
    public void setID(long id) {
        this.id = id;
    }

    public void setNewEmail(String newEmail){
        this.newEmail = newEmail;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filterType) {
        this.filter = filterType;
    }

    public String getEventCode(){
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }


    public String getNewEventName() {
        return newEventName;
    }

    public void setNewEventName(String newEventName) {
        this.newEventName = newEventName;
    }

    public String getNewEventLocation() {
        return newEventLocation;
    }

    public void setNewEventLocation(String newEventLocation) {
        this.newEventLocation = newEventLocation;
    }

    public String getNewEventDate() {
        return newEventDate;
    }

    public void setNewEventDate(String newEventDate) {
        this.newEventDate = newEventDate;
    }

    public String getNewEventBeginHour() {
        return newEventBeginHour;
    }

    public void setNewEventBeginHour(String newEventBeginHour) {
        this.newEventBeginHour = newEventBeginHour;
    }

    public String getNewEventEndHour() {
        return newEventEndHour;
    }

    public void setNewEventEndHour(String newEventEndHour) {
        this.newEventEndHour = newEventEndHour;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

}
