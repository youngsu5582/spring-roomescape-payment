package roomescape.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static roomescape.fixture.ReservationFixture.getNextDayReservation;
import static roomescape.fixture.ReservationTimeFixture.getNoon;
import static roomescape.fixture.ThemeFixture.getTheme1;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.exception.ErrorType;
import roomescape.exception.RoomescapeException;
import roomescape.reservation.controller.dto.ThemeResponse;
import roomescape.reservation.domain.ReservationTime;
import roomescape.reservation.domain.Theme;
import roomescape.reservation.domain.repository.MemberReservationRepository;
import roomescape.reservation.domain.repository.ReservationRepository;
import roomescape.reservation.domain.repository.ReservationTimeRepository;
import roomescape.reservation.domain.repository.ThemeRepository;
import roomescape.reservation.service.dto.ThemeCreate;
import roomescape.util.ServiceTest;


@DisplayName("테마 로직 테스트")
class ThemeServiceTest extends ServiceTest {
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    ThemeRepository themeRepository;
    @Autowired
    ReservationTimeRepository timeRepository;
    @Autowired
    MemberReservationRepository memberReservationRepository;
    @Autowired
    ThemeService themeService;

    @DisplayName("테마 조회에 성공한다.")
    @Test
    void findAll() {
        //given
        Theme theme = themeRepository.save(getTheme1());

        //when
        List<ThemeResponse> themes = themeService.findAllThemes();

        //then
        assertAll(() -> assertThat(themes).hasSize(1),
                () -> assertThat(themes.get(0).name()).isEqualTo(theme.getName()),
                () -> assertThat(themes.get(0).description()).isEqualTo(theme.getDescription()),
                () -> assertThat(themes.get(0).thumbnail()).isEqualTo(theme.getThumbnail()));
    }

    @DisplayName("테마 생성에 성공한다.")
    @Test
    void create() {
        //given
        String name = "name";
        String description = "description";
        String thumbnail = "thumbnail";
        long price = 15000L;
        ThemeCreate themeCreate = new ThemeCreate(name, description, thumbnail, price);

        //when
        ThemeResponse themeResponse = themeService.create(themeCreate);

        //then
        assertAll(() -> assertThat(themeResponse.name()).isEqualTo(name),
                () -> assertThat(themeResponse.thumbnail()).isEqualTo(thumbnail),
                () -> assertThat(themeResponse.description()).isEqualTo(description));
    }

    @DisplayName("테마 삭제에 성공한다.")
    @Test
    void delete() {
        //given
        Theme theme = getTheme1();
        themeRepository.save(theme);

        //when
        themeService.delete(theme.getId());

        //then
        assertThat(themeRepository.findAll()).isEmpty();
    }

    @DisplayName("예약이 존재하는 테마 삭제 시, 예외가 발생한다.")
    @Test
    void deleteThemeWithReservation() {
        //given
        ReservationTime time = timeRepository.save(getNoon());
        Theme theme = themeRepository.save(getTheme1());
        reservationRepository.save(getNextDayReservation(time, theme));

        //when & then
        assertThatThrownBy(() -> themeService.delete(theme.getId()))
                .isInstanceOf(RoomescapeException.class)
                .hasMessage(ErrorType.RESERVATION_NOT_DELETED.getMessage());
    }


    @DisplayName("startDate가 endDate보다 앞설 경우 예외가 발생한다.")
    @Test
    void endDatePriorToStartDate() {
        //given
        int limit = 10;
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now();

        //when & then
        assertThatThrownBy(() -> themeService.findPopularThemes(startDate, endDate, limit))
                .isInstanceOf(RoomescapeException.class)
                .hasMessage(ErrorType.INVALID_REQUEST_ERROR.getMessage());
    }
}
