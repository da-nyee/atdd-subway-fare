package wooteco.subway.line.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.exception.line.DuplicateColorException;
import wooteco.subway.exception.line.DuplicateNameException;
import wooteco.subway.line.dao.LineDao;
import wooteco.subway.line.dao.SectionDao;
import wooteco.subway.line.domain.Line;
import wooteco.subway.line.domain.Section;
import wooteco.subway.line.dto.*;
import wooteco.subway.station.application.StationService;
import wooteco.subway.station.domain.Station;
import wooteco.subway.station.dto.StationWithTransferLinesAndNextDistanceResponse;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class LineService {
    private final LineDao lineDao;
    private final SectionDao sectionDao;
    private final StationService stationService;

    public LineService(LineDao lineDao, SectionDao sectionDao, StationService stationService) {
        this.lineDao = lineDao;
        this.sectionDao = sectionDao;
        this.stationService = stationService;
    }

    public LineResponse saveLine(LineRequest request) {
        existsLine(request);
        Line persistLine = lineDao.insert(new Line(request.getName(), request.getColor()));
        persistLine.addSection(addInitSection(persistLine, request));
        return LineResponse.of(persistLine);
    }

    private Section addInitSection(Line line, LineRequest request) {
        if (request.getUpStationId() != null && request.getDownStationId() != null) {
            stationService.existsStation(request.getUpStationId());
            stationService.existsStation(request.getDownStationId());
            Station upStation = stationService.findStationById(request.getUpStationId());
            Station downStation = stationService.findStationById(request.getDownStationId());
            Section section = new Section(upStation, downStation, request.getDistance());
            return sectionDao.insert(line, section);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Line> findLines() {
        return lineDao.findAll();
    }

    @Transactional(readOnly = true)
    public List<LineWithTransferLinesAndNextDistanceResponse> findLinesWithTransferLinesAndNextDistance() {
        List<LineWithTransferLinesAndNextDistanceResponse> lineWithTransferLinesAndNextDistance = new ArrayList<>();

        for (Line line : lineDao.findAll()) {
            List<StationWithTransferLinesAndNextDistanceResponse> stations = findStations(line);
            lineWithTransferLinesAndNextDistance.add(LineWithTransferLinesAndNextDistanceResponse.of(line, stations));
        }
        return lineWithTransferLinesAndNextDistance;
    }

    @Transactional(readOnly = true)
    public LineWithTransferLinesAndNextDistanceResponse findLineWithTransferLinesAndNextDistance(Long id) {
        Line line = lineDao.findById(id);
        List<StationWithTransferLinesAndNextDistanceResponse> stations = findStations(line);
        return LineWithTransferLinesAndNextDistanceResponse.of(line, stations);
    }

    private List<StationWithTransferLinesAndNextDistanceResponse> findStations(Line line) {
        List<StationWithTransferLinesAndNextDistanceResponse> stationWithTransferLinesAndNextDistance = new ArrayList<>();

        for (Station station : line.getStations()) {
            int distance = line.getDistance(station);
            List<LineWithTransferLinesResponse> lineWithTransferLines =
                    lineDao.findTransferLinesByStationId(line.getId(), station.getId());

            stationWithTransferLinesAndNextDistance.add(
                    StationWithTransferLinesAndNextDistanceResponse.of(station, distance, lineWithTransferLines));
        }
        return stationWithTransferLinesAndNextDistance;
    }

    @Transactional(readOnly = true)
    public Line findLineById(Long id) {
        return lineDao.findById(id);
    }

    public LineUpdateResponse updateLine(Long id, LineRequest lineUpdateRequest) {
        existsLine(lineUpdateRequest);
        lineDao.update(new Line(id, lineUpdateRequest.getName(), lineUpdateRequest.getColor()));
        return LineUpdateResponse.of(lineDao.findById(id));
    }

    private void existsLine(LineRequest request) {
        if (lineDao.existsName(request.getName())) {
            throw new DuplicateNameException();
        }
        if (lineDao.existsColor(request.getColor())) {
            throw new DuplicateColorException();
        }
    }

    public void deleteLineById(Long id) {
        lineDao.deleteById(id);
    }

    public SectionAddResponse addLineStation(Long lineId, SectionRequest request) {
        Line line = findLineById(lineId);
        stationService.existsStation(request.getUpStationId());
        stationService.existsStation(request.getDownStationId());
        Station upStation = stationService.findStationById(request.getUpStationId());
        Station downStation = stationService.findStationById(request.getDownStationId());
        line.addSection(upStation, downStation, request.getDistance());

        sectionDao.deleteByLineId(lineId);
        sectionDao.insertSections(line);

        return findAddedSection(lineId, upStation, downStation);
    }

    private SectionAddResponse findAddedSection(Long lineId, Station upStation, Station downStation) {
        Line updatedLine = lineDao.findById(lineId);
        Section addedSection = updatedLine.getSections().getSections().stream()
                .filter(section -> section.getUpStation().equals(upStation) &&
                        section.getDownStation().equals(downStation))
                .findAny().get();

        return SectionAddResponse.of(addedSection);
    }

    public void removeLineStation(Long lineId, Long stationId) {
        Line line = findLineById(lineId);
        Station station = stationService.findStationById(stationId);
        line.removeSection(station);

        sectionDao.deleteByLineId(lineId);
        sectionDao.insertSections(line);
    }
}
