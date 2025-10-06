package br.com.on.fiap.hackathonprocessadorvideo.domain.model;

public record NotificacaoVideo(
        String idUsuario, String nomeVideo, String caminhoSaida, String situacao, String descricao, String email) {}
