package pt.ulisboa.tecnico.socialsoftware.tutor.question.domain;

import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.AnswerDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.CodeOrderAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.CodeOrderCorrectAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.CorrectAnswerDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.Visitor;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.Updator;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.CodeOrderQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.CodeOrderSlotDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.CodeOrderStatementAnswerDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.CodeOrderStatementQuestionDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.StatementAnswerDetailsDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.StatementQuestionDetailsDto;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.*;

@Entity
@DiscriminatorValue(Question.QuestionTypes.CODE_ORDER_QUESTION)
public class CodeOrderQuestion extends QuestionDetails {

    private Languages language;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "questionDetails", fetch = FetchType.LAZY, orphanRemoval = true)
    private final List<CodeOrderSlot> codeOrderSlots = new ArrayList<>();


    public CodeOrderQuestion() {
        super();
    }

    public CodeOrderQuestion(Question question, CodeOrderQuestionDto codeOrderQuestionDto) {
        super(question);
        update(codeOrderQuestionDto);
    }

    public Languages getLanguage() {
        return language;
    }

    public void setLanguage(Languages language) {
        this.language = language;
    }

    public List<CodeOrderSlot> getCodeOrderSlots() {
        return codeOrderSlots;
    }

    private void setCodeOrderSlots(List<CodeOrderSlotDto> codeOrderSlots) {
        if (codeOrderSlots.isEmpty()) {
            throw new TutorException(AT_LEAST_THREE_SLOTS_NEEDED);
        }
        if (codeOrderSlots.stream().filter(x -> x.getOrder() != null).count() < 3 ){
            throw new TutorException(AT_LEAST_THREE_SLOTS_NEEDED);
        }

        // Ensures some randomization when creating the slots ids.
        Collections.shuffle(codeOrderSlots);

        for (var codeOrderSlotDto : codeOrderSlots) {
            if (codeOrderSlotDto.getId() == null) {
                CodeOrderSlot codeOrderSlot = new CodeOrderSlot(codeOrderSlotDto);
                codeOrderSlot.setQuestionDetails(this);
                this.codeOrderSlots.add(codeOrderSlot);
            } else {
                CodeOrderSlot codeOrderSlot = getCodeOrderSlots()
                        .stream()
                        .filter(op -> op.getId().equals(codeOrderSlotDto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new TutorException(ORDER_SLOT_NOT_FOUND, codeOrderSlotDto.getId()));

                codeOrderSlot.setContent(codeOrderSlotDto.getContent());
                codeOrderSlot.setOrder(codeOrderSlotDto.getOrder());
            }
        }
    }

    public void update(CodeOrderQuestionDto questionDetails) {
        setLanguage(questionDetails.getLanguage());
        setCodeOrderSlots(questionDetails.getCodeOrderSlots());
    }


    @Override
    public CorrectAnswerDetailsDto getCorrectAnswerDetailsDto() {
        return new CodeOrderCorrectAnswerDto(this);
    }

    @Override
    public StatementQuestionDetailsDto getStatementQuestionDetailsDto() {
        return new CodeOrderStatementQuestionDetailsDto(this);
    }

    @Override
    public StatementAnswerDetailsDto getEmptyStatementAnswerDetailsDto() {
        return new CodeOrderStatementAnswerDetailsDto();
    }

    @Override
    public AnswerDetailsDto getEmptyAnswerDetailsDto() {
        return new CodeOrderAnswerDto();
    }

    @Override
    public QuestionDetailsDto getQuestionDetailsDto() {
        return new CodeOrderQuestionDto(this);
    }

    @Override
    public void delete() {
        super.delete();
        for (var slots : this.codeOrderSlots) {
            slots.delete();
        }
        this.codeOrderSlots.clear();
    }

    @Override
    public String getCorrectAnswerRepresentation() {
        return String.format("%d/%d", this.getCodeOrderSlots().size(), this.getCodeOrderSlots().size());
    }

    @Override
    public void update(Updator updator) {
        updator.update(this);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitQuestionDetails(this);
    }

    public void visitCodeOrderSlots(Visitor visitor) {
        for (var slot : this.getCodeOrderSlots()) {
            slot.accept(visitor);
        }
    }

    public CodeOrderSlot getCodeOrderSlotBySlotId(Integer slotId) {
        return this.codeOrderSlots
                .stream()
                .filter(slot1 -> slot1.getId().equals(slotId))
                .findAny()
                .orElseThrow(() -> new TutorException(QUESTION_ORDER_SLOT_MISMATCH, slotId));
    }
}
