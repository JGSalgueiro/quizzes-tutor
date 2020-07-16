package pt.ulisboa.tecnico.socialsoftware.tutor.question.domain;

import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.AnswerTypeDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.CorrectAnswerTypeDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.MultipleChoiceAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.MultipleChoiceCorrectAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.Visitor;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.MultipleChoiceQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionTypeDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.MultipleChoiceStatementQuestionDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.StatementQuestionDetailsDto;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.*;

@Entity
@DiscriminatorValue(Question.QuestionTypes.MULTIPLE_CHOICE_QUESTION)
public class MultipleChoiceQuestion extends QuestionType {
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "question", fetch = FetchType.EAGER, orphanRemoval = true)
    private final List<Option> options = new ArrayList<>();


    public MultipleChoiceQuestion() {

    }

    public MultipleChoiceQuestion(MultipleChoiceQuestionDto questionDto) {
        setOptions(questionDto.getOptions());
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<OptionDto> options) {
        if (options.stream().filter(OptionDto::isCorrect).count() != 1) {
            throw new TutorException(ONE_CORRECT_OPTION_NEEDED);
        }

        int index = 0;
        for (OptionDto optionDto : options) {
            if (optionDto.getId() == null) {
                optionDto.setSequence(index++);
                new Option(optionDto).setQuestion(this);
            } else {
                Option option = getOptions()
                        .stream()
                        .filter(op -> op.getId().equals(optionDto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new TutorException(OPTION_NOT_FOUND, optionDto.getId()));

                option.setContent(optionDto.getContent());
                option.setCorrect(optionDto.isCorrect());
            }
        }
    }

    public void addOption(Option option) {
        options.add(option);
    }

    public Integer getCorrectOptionId() {
        return this.getOptions().stream()
                .filter(Option::isCorrect)
                .findAny()
                .map(Option::getId)
                .orElse(null);
    }

    // TODO[is-has]: fix cast!
    public void update(QuestionDto questionDto) {
        setOptions(((MultipleChoiceQuestionDto)questionDto.getQuestion()).getOptions());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitQuestionType(this);
    }

    public void visitOptions(Visitor visitor) {
        for (Option option : this.getOptions()) {
            option.accept(visitor);
        }
    }

    @Override
    public CorrectAnswerTypeDto getCorrectAnswerDto() {
        return new MultipleChoiceCorrectAnswerDto(this);
    }

    @Override
    public StatementQuestionDetailsDto getStatementQuestionDetailsDto() {
        return new MultipleChoiceStatementQuestionDetailsDto(this);
    }

    @Override
    public AnswerTypeDto getEmptyAnswerTypeDto() {
        return new MultipleChoiceAnswerDto();
    }

    @Override
    public QuestionTypeDto getQuestionTypeDto() {
        return new MultipleChoiceQuestionDto(this);
    }

    @Override
    public Integer getCorrectAnswer() {
        return this.getOptions()
                .stream()
                .filter(Option::isCorrect)
                .findFirst().orElseThrow(() -> new TutorException(NO_CORRECT_OPTION))
                .getSequence();
    }

    @Override
    public void delete(QuestionService questionService) {
        questionService.deleteQuestion(this);
    }

    //TODO[is->has]: Update toString()
    /*@Override
    public String toString() {
        return "Question{" +
                "id=" + getId() +
                ", key=" + getKey() +
                ", content='" + getContent() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", numberOfAnswers=" + getNumberOfAnswers() +
                ", numberOfCorrect=" + getNumberOfAnswers() +
                ", status=" + getStatus() +
                ", image=" + getImage() +
                ", options=" + getOptions() +
                ", topics=" + getTopics() +
                '}';
    }*/
}
