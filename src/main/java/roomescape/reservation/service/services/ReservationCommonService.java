package roomescape.reservation.service.services;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.ErrorType;
import roomescape.exception.RoomescapeException;
import roomescape.member.domain.Member;
import roomescape.member.domain.repository.MemberRepository;
import roomescape.reservation.domain.MemberReservation;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.domain.ReservationTime;
import roomescape.reservation.domain.Theme;
import roomescape.reservation.domain.repository.MemberReservationRepository;
import roomescape.reservation.domain.repository.ReservationRepository;
import roomescape.reservation.domain.repository.ReservationTimeRepository;
import roomescape.reservation.domain.repository.ThemeRepository;
import roomescape.reservation.service.dto.ReservationCreate;

@Service
@Transactional(readOnly = true)
public class ReservationCommonService {

    private final ReservationRepository reservationRepository;

    private final ReservationTimeRepository reservationTimeRepository;

    private final ThemeRepository themeRepository;

    private final MemberRepository memberRepository;

    private final MemberReservationRepository memberReservationRepository;

    public ReservationCommonService(ReservationRepository reservationRepository,
                                    ReservationTimeRepository reservationTimeRepository,
                                    ThemeRepository themeRepository,
                                    MemberRepository memberRepository,
                                    MemberReservationRepository memberReservationRepository) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.memberRepository = memberRepository;
        this.memberReservationRepository = memberReservationRepository;
    }

    public void validateDuplicatedReservation(Reservation reservation, Member member) {
        if (memberReservationRepository.existsByReservationAndMember(reservation, member)) {
            throw new RoomescapeException(ErrorType.DUPLICATED_RESERVATION_ERROR);
        }
    }

    public void validateMemberReservation(MemberReservation memberReservation, Member member) {
        if (memberReservation.isNotEqualMember(member)) {
            throw new RoomescapeException(ErrorType.NOT_A_RESERVATION_MEMBER);
        }
    }

    public void delete(Member member, MemberReservation memberReservation) {
        if (!memberReservation.canDelete(member)) {
            throw new RoomescapeException(ErrorType.NOT_A_RESERVATION_MEMBER);
        }
        memberReservationRepository.deleteById(memberReservation.getId());
    }

    public void validatePastReservation(Reservation reservation) {
        if (reservation.isPast()) {
            throw new RoomescapeException(ErrorType.INVALID_REQUEST_ERROR);
        }
    }

    private boolean isReservationConfirmed(Reservation reservation) {
        return memberReservationRepository.existsByReservationAndReservationStatus(reservation,
                ReservationStatus.APPROVED);
    }

    private ReservationTime getReservationTime(long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new RoomescapeException(ErrorType.RESERVATION_TIME_NOT_FOUND));
    }

    private Theme getTheme(long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(ErrorType.THEME_NOT_FOUND));
    }

    public Member getMember(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RoomescapeException(ErrorType.MEMBER_NOT_FOUND));
    }

    private Reservation getReservation(LocalDate date, ReservationTime time, Theme theme) {
        return reservationRepository.findReservationByDateAndTimeAndTheme(date, time, theme)
                .orElseGet(() -> reservationRepository.save(new Reservation(date, time, theme)));
    }

    public MemberReservation getMemberReservation(long memberReservationId) {
        return memberReservationRepository.findById(memberReservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorType.MEMBER_RESERVATION_NOT_FOUND));
    }

    @Transactional
    public MemberReservation create(ReservationCreate reservationCreate) {
        ReservationTime reservationTime = getReservationTime(reservationCreate.timeId());
        Theme theme = getTheme(reservationCreate.themeId());
        Member member = getMember(reservationCreate.memberId());
        Reservation reservation = getReservation(reservationCreate.date(), reservationTime, theme);

        validatePastReservation(reservation);
        validateDuplicatedReservation(reservation, member);

        if (isReservationConfirmed(reservation)) {
            return memberReservationRepository.save(new MemberReservation(member, reservation, ReservationStatus.PENDING));
        }

        return memberReservationRepository.save(new MemberReservation(member, reservation, ReservationStatus.APPROVED));
    }
}
