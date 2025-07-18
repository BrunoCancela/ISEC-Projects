package pt.isec.pd.springboot.models;

public class UserConfig {

    private String nome;
    private Long numeroIdentificacao;
    private String email;
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getNumeroIdentificacao() {
        return numeroIdentificacao;
    }
    public void setNumeroIdentificacao(Long numeroIdentificacao) {
        this.numeroIdentificacao = numeroIdentificacao;
    }
    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }
}