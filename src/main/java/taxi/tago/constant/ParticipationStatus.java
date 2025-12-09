package taxi.tago.constant;

public enum ParticipationStatus {
    WAITING,  // 요청 보내고 대기 중
    ACCEPTED, // 수락됨, 채팅방 입장 가능
    NONE,      // 같이 타기 요청 보내기 가능
    KICKED // 강퇴
}