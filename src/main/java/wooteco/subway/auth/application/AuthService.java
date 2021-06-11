package wooteco.subway.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.auth.dto.TokenRequest;
import wooteco.subway.auth.dto.TokenResponse;
import wooteco.subway.auth.infrastructure.JwtTokenProvider;
import wooteco.subway.exception.member.InvalidLoginException;
import wooteco.subway.member.dao.MemberDao;
import wooteco.subway.member.domain.GuestMember;
import wooteco.subway.member.domain.LoginMember;
import wooteco.subway.member.domain.Member;
import wooteco.subway.member.domain.User;

@Service
@Transactional
public class AuthService {
    private final MemberDao memberDao;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(MemberDao memberDao, JwtTokenProvider jwtTokenProvider) {
        this.memberDao = memberDao;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenResponse login(TokenRequest request) {
        try {
            Member member = memberDao.findByEmail(request.getEmail());
            member.checkPassword(request.getPassword());
        } catch (Exception e) {
            throw new InvalidLoginException();
        }
        String token = jwtTokenProvider.createToken(request.getEmail());
        return new TokenResponse(token);
    }

    public User findMemberByToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return new GuestMember();
        }

        String email = jwtTokenProvider.getPayload(token);
        try {
            Member member = memberDao.findByEmail(email);
            return new LoginMember(member.getId(), member.getEmail(), member.getAge());
        } catch (Exception e) {
            return new GuestMember();
        }
    }
}
